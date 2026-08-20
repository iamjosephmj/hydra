# Attack 3 — KernelSU / Magisk module: on-disk lib replacement

A rooted attacker installs a KSU/Magisk module that **bind-mounts a patched copy
of the protected native lib** over the app's extracted one, so the app loads the
attacker's neutered library. The protected runtime bakes its native code hash at
build time, so the replaced `.so` mismatches at runtime and the app self-terminates.

## Build + install
```bash
bash make-payload.sh                       # -> payload/libdicore.so (patched)
zip -r hydra_neutralize.zip module.prop post-fs-data.sh payload/
adb push hydra_neutralize.zip /data/local/tmp/
adb shell su -c "ksud module install /data/local/tmp/hydra_neutralize.zip" && adb reboot
```
Then launch `com.example.hydrasample` and check it does not survive (see ../README.md).
Requires root + KernelSU (or Magisk); inert against any other app.
