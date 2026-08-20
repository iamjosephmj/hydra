// di-zygisk-probe — a native Zygisk module that goes BELOW LSPosed's Java/ART
// layer: it runs at Zygote fork, waits for the target app to load libdicore.so,
// then probes / tampers the RASP native library in memory. Scoped to
// com.example.hydrasample only; every other process is a no-op (and unloads the module).
//
// Modes (adb shell setprop hydra.zygisk.mode <mode>):
//   probe      (default) — locate libdicore.so + JNI_OnLoad + the .text range and
//                          log them. Pure reconnaissance, no tamper.
//   text-tamper          — flip 4 bytes of JNI_OnLoad's prologue in the r-xp
//                          .text (JNI_OnLoad already ran, so this is execution-
//                          safe) → the native .text-hash detector (G2) should
//                          fire `native_text_hash_mismatch`. Tests whether native
//                          self-integrity catches a native in-memory .text tamper
//                          that LSPosed structurally cannot perform.
#include <sys/types.h>   // dev_t / ino_t — required before zygisk.hpp
#include "zygisk.hpp"

#include <android/log.h>
#include <dlfcn.h>
#include <pthread.h>
#include <sys/mman.h>
#include <sys/system_properties.h>
#include <unistd.h>
#include <cerrno>
#include <cstdint>
#include <cstdio>
#include <cstring>

#define TARGET "com.example.hydrasample"
#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "DI-ZYGISK", __VA_ARGS__)

using zygisk::Api;
using zygisk::AppSpecializeArgs;
using zygisk::ModuleBase;

namespace {

// Read /proc/self/maps for the r-xp segment containing `addr`; fill [start,end).
bool text_range_for(uintptr_t addr, uintptr_t *start, uintptr_t *end, char *path, size_t plen) {
    FILE *f = fopen("/proc/self/maps", "re");
    if (!f) return false;
    char line[512];
    bool found = false;
    while (fgets(line, sizeof(line), f)) {
        uintptr_t s, e;
        char perms[8] = {0};
        char p[256] = {0};
        int n = sscanf(line, "%lx-%lx %7s %*s %*s %*s %255[^\n]", &s, &e, perms, p);
        if (n < 3) continue;
        if (perms[0] == 'r' && perms[2] == 'x' && addr >= s && addr < e) {
            *start = s; *end = e;
            if (path && plen) { strncpy(path, p, plen - 1); path[plen - 1] = 0; }
            found = true;
            break;
        }
    }
    fclose(f);
    return found;
}

// Write 4 fixed bytes into JNI_OnLoad's prologue (idempotent so it can be
// re-applied). JNI_OnLoad already ran, so overwriting it is execution-safe.
bool patch_text(uintptr_t jni_onload) {
    long pagesz = sysconf(_SC_PAGESIZE);
    void *page = (void *)(jni_onload & ~(uintptr_t)(pagesz - 1));
    if (mprotect(page, pagesz, PROT_READ | PROT_WRITE | PROT_EXEC) != 0) return false;
    static const uint8_t kPatch[4] = {0xDE, 0xAD, 0xBE, 0xEF};
    volatile uint8_t *p = (uint8_t *)jni_onload;
    for (int i = 0; i < 4; i++) p[i] = kPatch[i];
    __builtin___clear_cache((char *)jni_onload, (char *)jni_onload + 4);
    mprotect(page, pagesz, PROT_READ | PROT_EXEC);
    return true;
}

void *probe_thread(void *) {
    char mode[PROP_VALUE_MAX] = {0};
    if (__system_property_get("hydra.zygisk.mode", mode) <= 0) strcpy(mode, "probe");

    void *h = nullptr;
    for (int i = 0; i < 5000 && !h; i++) {          // tight poll: win the race vs the protected lib’s integrity scan
        h = dlopen("libdicore.so", RTLD_NOLOAD | RTLD_NOW);
        if (!h) usleep(2 * 1000);
    }
    if (!h) { LOG("libdicore.so never loaded — giving up (mode=%s)", mode); return nullptr; }

    void *jni_onload = dlsym(h, "JNI_OnLoad");
    uintptr_t ts = 0, te = 0;
    char path[256] = {0};
    if (jni_onload) text_range_for((uintptr_t)jni_onload, &ts, &te, path, sizeof(path));
    LOG("libdicore loaded: JNI_OnLoad=%p .text=[%lx-%lx] (%lu KB) path=%s mode=%s",
        jni_onload, ts, te, (te - ts) / 1024, path, mode);

    if (strcmp(mode, "text-tamper") == 0 && jni_onload) {
        // Patch immediately and HOLD it for ~3s (re-applying every 3ms) so the
        // patch is present whenever the protected lib runs its .text scan — no race.
        bool ok = false;
        for (int i = 0; i < 1000; i++) { ok = patch_text((uintptr_t)jni_onload); usleep(3 * 1000); }
        LOG("text-tamper: %s JNI_OnLoad@%p prologue held patched — .text should differ from the baked hash",
            ok ? "patched" : "FAILED", (void *)jni_onload);
    }
    return nullptr;
}

}  // namespace

class DiProbe : public ModuleBase {
public:
    void onLoad(Api *api, JNIEnv *env) override {
        api_ = api;
        env_ = env;
    }

    void preAppSpecialize(AppSpecializeArgs *args) override {
        target_ = false;
        if (!args || !args->nice_name) return;
        const char *name = env_->GetStringUTFChars(args->nice_name, nullptr);
        if (name) {
            target_ = (strcmp(name, TARGET) == 0);
            env_->ReleaseStringUTFChars(args->nice_name, name);
        }
        if (!target_) {
            // Not our target: unload the module from this process entirely.
            api_->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
        }
    }

    void postAppSpecialize(const AppSpecializeArgs *) override {
        if (!target_) return;
        pthread_t t;
        if (pthread_create(&t, nullptr, probe_thread, nullptr) == 0) {
            pthread_detach(t);
        }
    }

private:
    Api *api_ = nullptr;
    JNIEnv *env_ = nullptr;
    bool target_ = false;
};

REGISTER_ZYGISK_MODULE(DiProbe)
