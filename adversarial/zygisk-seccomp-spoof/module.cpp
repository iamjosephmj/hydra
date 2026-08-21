// zygisk-seccomp-spoof (app side) — red-team, research / transparency only. Inert against any app but the sample.
//
// Installs a SECCOMP_RET_USER_NOTIF filter on the target that traps openat/openat2,
// then LEAKS the listener fd so an external ROOT supervisor (sup_daemon, run via su)
// can grab it with pidfd_getfd and adjudicate each open — intercepting /proc reads
// at the syscall boundary, below libc AND raw SVC #0. (ReZygisk's connectCompanion is
// broken on this rig, hence pidfd delivery.) Scoped to com.example.hydrasample
// (override: setprop hydra.rt.sctarget <pkg>).

#include <sys/types.h>
#include "zygisk.hpp"
#include <android/log.h>
#include <linux/audit.h>
#include <linux/filter.h>
#include <linux/seccomp.h>
#include <sys/prctl.h>
#include <sys/syscall.h>
#include <sys/system_properties.h>
#include <unistd.h>
#include <cstring>

#define TARGET "com.example.hydrasample"
#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "DI-RT-SECCOMP", __VA_ARGS__)
#ifndef SECCOMP_FILTER_FLAG_NEW_LISTENER
#define SECCOMP_FILTER_FLAG_NEW_LISTENER (1UL << 3)
#endif
#ifndef SECCOMP_RET_USER_NOTIF
#define SECCOMP_RET_USER_NOTIF 0x7fc00000U
#endif
#ifndef __NR_openat2
#define __NR_openat2 437
#endif

namespace {
void install_filter() {
    prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0);
    struct sock_filter filter[] = {
        BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, arch)),
        BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, AUDIT_ARCH_AARCH64, 1, 0),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW),
        BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, nr)),
        BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, __NR_openat, 2, 0),
        BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, __NR_openat2, 1, 0),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_USER_NOTIF),
    };
    struct sock_fprog prog { (unsigned short)(sizeof(filter)/sizeof(filter[0])), filter };
    long nfd = syscall(__NR_seccomp, SECCOMP_SET_MODE_FILTER, SECCOMP_FILTER_FLAG_NEW_LISTENER, &prog);
    if (nfd < 0) LOG("NEW_LISTENER failed errno=%d", (int)nfd);
    else LOG("installed user-notif filter, leaked listener fd=%ld for sup_daemon", nfd);
    // Do NOT close nfd — the root sup_daemon pidfd_getfd's it.
}
}  // namespace

using zygisk::Api; using zygisk::AppSpecializeArgs; using zygisk::ModuleBase;
class SeccompSpoof : public ModuleBase {
public:
    void onLoad(Api* api, JNIEnv* env) override { api_ = api; env_ = env; }
    void preAppSpecialize(AppSpecializeArgs* args) override {
        target_ = false;
        char want[PROP_VALUE_MAX] = {0};
        if (__system_property_get("hydra.rt.sctarget", want) <= 0) strcpy(want, TARGET);
        if (!args || !args->nice_name) return;
        const char* n = env_->GetStringUTFChars(args->nice_name, nullptr);
        if (n) { target_ = !strcmp(n, want); env_->ReleaseStringUTFChars(args->nice_name, n); }
        if (!target_) api_->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
    }
    void postAppSpecialize(const AppSpecializeArgs*) override { if (target_) install_filter(); }
private:
    Api* api_ = nullptr; JNIEnv* env_ = nullptr; bool target_ = false;
};
REGISTER_ZYGISK_MODULE(SeccompSpoof)
