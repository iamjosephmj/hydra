// zygisk-ret-patch — red-team module — research / transparency only. Inert against any app but the sample.
//
// The generic native neutralization vehicle: `ret`-patch the prologue of an internal
// libdicore.so function so it returns immediately. Only JNI_OnLoad is exported (the
// anchors are RegisterNatives-bound, hidden-visibility, OLLVM-flattened), so an
// attacker cannot dlsym the target — they must recover its OFFSET by reverse-
// engineering the shipped .so. We simulate that determined attacker: feed the offset
// (from `nm` on the matching trail build — see offsets.sh) and this patches
// base+offset in memory and holds it.
//
// This is the vehicle for several neutralizations, selected purely by which offset
// you feed (adb shell setprop):
//   hydra.rt.offset  <hex>   file offset of the target function (from offsets.sh)
//   hydra.rt.label   <name>  cosmetic label for the log
//
//   target        certifies
//   nat_o         funnel-at-entry: skip the whole sweep (does anything still kill?)
//   collect       the OPEN funnel residual: force 0 CRITICAL
//   di_enforce_kill  B1: the in-process kill is neutered -> child must still detonate
//   di_condemn    B1 guard: ret-hooking the new kill trigger -> must be a detected
//                 CRITICAL (prologue watch) AND the watchdog verdict must still flip
//   di_detonate   the in-process crash -> must not save the process (watchdog kills)
//
// Expected if the hardening holds: patching nat_o/collect is caught by the prologue
// self-hook watch OR the process still dies via the watchdog; patching the kill path
// leaves B1's child-side verdict kill intact. A SURVIVE is a private finding to fix.
// Scoped to com.example.hydrasample; inert elsewhere.

#include <sys/types.h>
#include "zygisk.hpp"

#include <android/log.h>
#include <dlfcn.h>
#include <pthread.h>
#include <sys/mman.h>
#include <sys/system_properties.h>
#include <unistd.h>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>

#define TARGET "com.example.hydrasample"
#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "DI-RT-RETPATCH", __VA_ARGS__)

namespace {

// Load base (bias) of libdicore.so. NOTE: the runtime loads the lib straight from
// the APK (base.apk!/lib/...), so /proc/self/maps shows the APK path, not
// "libdicore.so" — a maps grep misses it. Resolve via the loader instead: dlopen
// RTLD_NOLOAD gets the handle once loaded, and dladdr(JNI_OnLoad) hands back the
// shared object's base (dli_fbase). runtime addr = base + symbol vaddr (from nm).
uintptr_t lib_base(const char * /*unused*/) {
    void *h = dlopen("libdicore.so", RTLD_NOLOAD | RTLD_NOW);
    if (!h) return 0;
    void *anchor = dlsym(h, "JNI_OnLoad");
    if (!anchor) return 0;
    Dl_info info{};
    if (dladdr(anchor, &info) == 0 || !info.dli_fbase) return 0;
    return reinterpret_cast<uintptr_t>(info.dli_fbase);
}

// aarch64 `RET` (x30). For a void/ignored-return function this cleanly returns; for
// an int function the return value is undefined, which is fine for the sweep entry
// (its caller ignores the count). Held (re-applied) so it is present at call time.
bool ret_patch(uintptr_t addr) {
#if defined(__aarch64__)
    static const uint32_t kRet = 0xD65F03C0u;
    long pg = sysconf(_SC_PAGESIZE);
    void *page = (void *)(addr & ~(uintptr_t)(pg - 1));
    if (mprotect(page, pg, PROT_READ | PROT_WRITE | PROT_EXEC) != 0) return false;
    *(volatile uint32_t *)addr = kRet;
    __builtin___clear_cache((char *)addr, (char *)addr + 4);
    mprotect(page, pg, PROT_READ | PROT_EXEC);
    return true;
#else
    (void)addr; return false;   // this rig is arm64
#endif
}

void *patch_thread(void *) {
    char offs[PROP_VALUE_MAX] = {0}, label[PROP_VALUE_MAX] = {0};
    __system_property_get("hydra.rt.offset", offs);
    if (__system_property_get("hydra.rt.label", label) <= 0) strcpy(label, "?");
    uintptr_t off = offs[0] ? strtoull(offs, nullptr, 16) : 0;
    if (!off) { LOG("no hydra.rt.offset set — nothing to patch"); return nullptr; }

    // Wait for libdicore.so, then race: apply the patch and HOLD it (re-apply every
    // 2 ms) so it is present whenever the target would run.
    uintptr_t base = 0;
    for (int i = 0; i < 5000 && !base; i++) { base = lib_base("libdicore.so"); if (!base) usleep(2000); }
    if (!base) { LOG("libdicore.so never mapped"); return nullptr; }

    uintptr_t addr = base + off;
    LOG("target '%s' off=0x%lx base=0x%lx addr=0x%lx — ret-patching + holding", label, off, base, addr);
    int ok = 0;
    for (int i = 0; i < 4000; i++) { ok += ret_patch(addr) ? 1 : 0; usleep(2000); }
    LOG("held ret-patch on '%s' (%d applications) — target should now be a no-op", label, ok);
    return nullptr;
}

}  // namespace

using zygisk::Api;
using zygisk::AppSpecializeArgs;
using zygisk::ModuleBase;

class RetPatch : public ModuleBase {
public:
    void onLoad(Api *api, JNIEnv *env) override { api_ = api; env_ = env; }
    void preAppSpecialize(AppSpecializeArgs *args) override {
        target_ = false;
        if (!args || !args->nice_name) return;
        char want[PROP_VALUE_MAX] = {0};
        if (__system_property_get("hydra.rt.target", want) <= 0) strcpy(want, TARGET);
        const char *n = env_->GetStringUTFChars(args->nice_name, nullptr);
        if (n) { target_ = !strcmp(n, want); env_->ReleaseStringUTFChars(args->nice_name, n); }
        if (!target_) api_->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
    }
    void postAppSpecialize(const AppSpecializeArgs *) override {
        if (!target_) return;
        pthread_t t;
        if (pthread_create(&t, nullptr, patch_thread, nullptr) == 0) pthread_detach(t);
    }
private:
    Api *api_ = nullptr; JNIEnv *env_ = nullptr; bool target_ = false;
};

REGISTER_ZYGISK_MODULE(RetPatch)
