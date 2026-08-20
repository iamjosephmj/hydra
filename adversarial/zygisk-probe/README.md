# Attack 2 — Zygisk: in-memory native `.text` patch

The deepest on-device tamper: a **native Zygisk module** loads at Zygote fork,
waits for the protected native lib to map in, and rewrites its `.text` **in
memory** (`hydra.zygisk.mode=text-tamper`) — no on-disk change, no re-sign. It
patches one instruction (NOP→YIELD) inside the code region and *holds* the patch,
so it is present whenever the protected lib runs its integrity scan. The runtime
re-hashes its live in-memory `.text`, sees the drift, and self-terminates.

`setprop hydra.zygisk.mode probe` only reports the lib's load address / `.text`
range (no tamper) for calibration.

## Build + install
```bash
NDK=$HOME/Android/Sdk/ndk/<ver> bash build.sh        # -> build/hydra-zygisk-probe.zip
adb push build/hydra-zygisk-probe.zip /data/local/tmp/
adb shell su -c "magisk --install-module /data/local/tmp/hydra-zygisk-probe.zip" && adb reboot
adb shell su -c "setprop hydra.zygisk.mode text-tamper"
```
Then launch `com.example.hydrasample` and check it does not survive (see ../README.md).
Requires root + Zygisk (Magisk or KernelSU-Next); inert against any other app.
