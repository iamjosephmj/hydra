// zygisk-survive-crash — red-team module — research / transparency only. Inert against any app but the sample.
//
// Unlike the shallow .text-patch probe (hydra/adversarial/zygisk-probe), this does
// not try to avoid detection — it lets the runtime detect tampering and then tries
// to SURVIVE THE KILL. It is the stress test for the "crashing part": every layer
// of the enforcement kill is attacked at once, and we log whether the process stays
// alive. Scoped to com.example.hydrasample; a no-op (and self-unloading) elsewhere.
//
// The kill it fights (from the enforcement design):
//   1. di_detonate  — an in-process, syscall-free wild-memory-write loop => SIGSEGV
//   2. the forked watchdog child — PTRACE_ATTACHes the parent and corrupts its
//      PC/SP => an organic SIGSEGV from *another process*
//   3. escalation ladder — SIGSEGV -> SIGABRT -> SIGBUS -> SIGKILL (uncatchable)
//   4. B1 — the watchdog is the primary enforcer; di_condemn() is set before the
//      hookable di_enforce_kill, so the child kills on the heartbeat verdict even
//      if the in-process path is neutered.
//
// Attack layers (adb shell setprop hydra.rt.mode <mode>):
//   catch-signals  install alt-stack handlers for SIGSEGV/SIGBUS/SIGABRT/SIGILL/SIGFPE
//                  that swallow the fault and siglongjmp back to a survive loop.
//   kill-watchdog  find the forked watchdog child (our only child process with no
//                  thread of its own) and SIGKILL it on a tight loop (it re-forks).
//   deny-ptrace    race PR_SET_DUMPABLE back to 0 so the watchdog's PTRACE_ATTACH
//                  is denied (EPERM) — defeat the *organic* external kill.
//   full (default) all of the above, concurrently.
//
// Expected outcome if the hardening holds: the app STILL dies — SIGKILL is
// uncatchable, di_detonate re-faults faster than a handler can longjmp out, and the
// re-forking watchdog + B1 verdict make the external kill independent of anything an
// in-process attacker can reach. This module measures exactly how close to survival
// an all-out attacker gets, and surfaces any layer that IS defeatable.

#include <sys/types.h>   // dev_t / ino_t — required before zygisk.hpp
#include "zygisk.hpp"

#include <android/log.h>
#include <dirent.h>
#include <dlfcn.h>
#include <pthread.h>
#include <setjmp.h>
#include <signal.h>
#include <sys/prctl.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <sys/system_properties.h>
#include <unistd.h>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <initializer_list>

#define TARGET "com.example.hydrasample"
#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "DI-RT-SURVIVE", __VA_ARGS__)

namespace {

volatile sig_atomic_t g_caught = 0;
sigjmp_buf g_jmp;
bool g_jmp_armed = false;

// Swallow a fatal signal: count it, and if we have a recovery point, jump back to
// the survive loop instead of dying. For the di_detonate wild-write loop this only
// buys a moment — the loop's next store re-faults immediately — but it measures
// whether the in-process detonate is catchable at all.
void on_fatal(int sig, siginfo_t *info, void *uctx) {
    (void)uctx;
    g_caught++;
    if ((g_caught % 200) == 1)
        LOG("caught signal %d (addr=%p) count=%ld — attempting to survive",
            sig, info ? info->si_addr : nullptr, (long)g_caught);
    if (g_jmp_armed) siglongjmp(g_jmp, sig);
    // no recovery point: last resort — reset to default and return (will re-raise).
    signal(sig, SIG_DFL);
}

void install_signal_catchers() {
    static char altstack[SIGSTKSZ * 4];
    stack_t ss{};
    ss.ss_sp = altstack;
    ss.ss_size = sizeof(altstack);
    ss.ss_flags = 0;
    sigaltstack(&ss, nullptr);

    struct sigaction sa{};
    sa.sa_sigaction = on_fatal;
    sa.sa_flags = SA_SIGINFO | SA_ONSTACK | SA_NODEFER;
    sigemptyset(&sa.sa_mask);
    for (int s : {SIGSEGV, SIGBUS, SIGABRT, SIGILL, SIGFPE}) sigaction(s, &sa, nullptr);
    LOG("installed alt-stack catchers for SEGV/BUS/ABRT/ILL/FPE");
}

// The forked watchdog is a child of our pid whose /proc/<c>/stat shows it is our
// child. Kill every such child on a tight loop; if the kill still lands, the
// watchdog's re-fork + B1 verdict defeated us.
int reap_watchdog_children(pid_t self) {
    int killed = 0;
    DIR *d = opendir("/proc");
    if (!d) return 0;
    struct dirent *e;
    while ((e = readdir(d))) {
        pid_t c = (pid_t)atoi(e->d_name);
        if (c <= 0 || c == self) continue;
        char p[64];
        snprintf(p, sizeof(p), "/proc/%d/stat", c);
        FILE *f = fopen(p, "re");
        if (!f) continue;
        // field 4 of stat = ppid (after "pid (comm) state")
        int pid = 0, ppid = 0;
        char comm[256], state;
        if (fscanf(f, "%d (%255[^)]) %c %d", &pid, comm, &state, &ppid) == 4 && ppid == self) {
            if (kill(c, SIGKILL) == 0) killed++;
        }
        fclose(f);
    }
    closedir(d);
    return killed;
}

void *attack_thread(void *) {
    char mode[PROP_VALUE_MAX] = {0};
    if (__system_property_get("hydra.rt.mode", mode) <= 0) strcpy(mode, "full");
    bool want_sig   = !strcmp(mode, "catch-signals") || !strcmp(mode, "full");
    bool want_reap  = !strcmp(mode, "kill-watchdog")  || !strcmp(mode, "full");
    bool want_deny  = !strcmp(mode, "deny-ptrace")    || !strcmp(mode, "full");
    LOG("attack start mode=%s (sig=%d reap=%d deny=%d)", mode, want_sig, want_reap, want_deny);

    if (want_sig) install_signal_catchers();

    pid_t self = getpid();
    // Survive loop: siglongjmp lands here. Keep the process alive as long as we can,
    // while continuously attacking the watchdog + racing dumpable.
    if (want_sig) { if (sigsetjmp(g_jmp, 1) != 0) { /* recovered from a fault */ } g_jmp_armed = true; }

    unsigned long ticks = 0;
    for (;;) {
        if (want_deny) prctl(PR_SET_DUMPABLE, 0, 0, 0, 0);  // race the kill's PR_SET_DUMPABLE(1)
        if (want_reap) {
            int k = reap_watchdog_children(self);
            if (k) LOG("SIGKILLed %d watchdog child(ren) at tick %lu", k, ticks);
        }
        // Prove liveness periodically so the trail shows exactly how long we lasted.
        if ((ticks % 500) == 0)
            LOG("STILL ALIVE tick=%lu caught=%ld (if you see this after the verdict, a layer was defeated)",
                ticks, (long)g_caught);
        ticks++;
        usleep(1000);  // 1ms — tight enough to race the ~1.5s grace window
    }
    return nullptr;
}

}  // namespace

using zygisk::Api;
using zygisk::AppSpecializeArgs;
using zygisk::ModuleBase;

class SurviveCrash : public ModuleBase {
public:
    void onLoad(Api *api, JNIEnv *env) override { api_ = api; env_ = env; }

    void preAppSpecialize(AppSpecializeArgs *args) override {
        target_ = false;
        if (!args || !args->nice_name) return;
        const char *name = env_->GetStringUTFChars(args->nice_name, nullptr);
        if (name) { target_ = !strcmp(name, TARGET); env_->ReleaseStringUTFChars(args->nice_name, name); }
        if (!target_) api_->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
    }

    void postAppSpecialize(const AppSpecializeArgs *) override {
        if (!target_) return;
        pthread_t t;
        if (pthread_create(&t, nullptr, attack_thread, nullptr) == 0) pthread_detach(t);
    }

private:
    Api *api_ = nullptr;
    JNIEnv *env_ = nullptr;
    bool target_ = false;
};

REGISTER_ZYGISK_MODULE(SurviveCrash)
