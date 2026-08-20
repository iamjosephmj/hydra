# Attack 4 — LSPosed ART method hook

An in-process ART hook installed at Zygote fork (no frida artifacts). Hooking a
monitored ART method (`String#length`) rewrites its `ArtMethod` — the primitive
the protected runtime's ART-integrity detector watches — so a protected app
self-terminates. The callback is a no-op; the installation is the attack.

## Build + install
```bash
./gradlew :adversarial:lsposed-module:assembleRelease   # or build this dir standalone
adb install adversarial/lsposed-module/build/outputs/apk/release/*.apk
```
Then in **LSPosed Manager**: enable the module, scope it to `com.example.hydrasample`,
reboot. Launch the sample and check it does not survive (see ../README.md).
Requires root + Zygisk + LSPosed; the `de.robv.android.xposed:api:82` dependency is
compile-only (provided by LSPosed at runtime).
