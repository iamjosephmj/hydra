# zygisk-ret-patch

The generic **native neutralization vehicle**: `ret`-patch the prologue of an
internal `libdicore.so` function so it returns immediately. Only `JNI_OnLoad` is
exported (the detector anchors are `RegisterNatives`-bound, hidden-visibility,
OLLVM-flattened), so an attacker cannot `dlsym` a target — they must recover its
**offset** by reverse-engineering the shipped `.so`. This module simulates that
determined attacker: you feed the offset and it patches `base+offset` in memory and
holds it.

One vehicle, many attacks — selected purely by which offset you feed:

| `setprop hydra.rt.label` → target | What it neuters | Expected result (hardened) |
|---|---|---|
| `nat_o` | funnel-at-entry: skip the whole sweep | caught by the prologue self-hook watch, or the watchdog still kills |
| `collect` | the open funnel: force 0 CRITICAL | same as above |
| `di_enforce_kill` | B1: the in-process kill | the forked watchdog's heartbeat verdict still detonates |
| `di_condemn` | B1 guard: the new kill trigger | a detected CRITICAL (prologue watch) **and** the verdict still flips |
| `di_detonate` | the in-process crash loop | must not save the process (watchdog kills from outside) |

Every case is expected to **still kill** on the shipped obfuscated build — a survival
is an internal finding to fix before the result is published here. On the real release
the `.so` is stripped + OLLVM-flattened, so recovering an internal offset at all is
impractical, which is the intended defense; this module is run against an *unstripped
trail build* to exercise the kill paths directly.

## Usage

```bash
# 1. Recover offsets from an UNSTRIPPED trail libdicore.so (simulates RE of the shipped lib)
./offsets.sh path/to/unstripped/libdicore.so     # prints "0x<offset>  <symbol>"

# 2. Build + install the module (arm64 + x86_64)
./build.sh                                        # -> build/di-rt-retpatch.zip
adb push build/di-rt-retpatch.zip /sdcard/Download/
# Magisk/KernelSU -> Modules -> Install from storage -> reboot

# 3. Point it at a target and launch
adb shell setprop hydra.rt.offset 0x<offset>
adb shell setprop hydra.rt.label  di_enforce_kill
adb shell am start -n com.example.hydrasample/.MainActivity
adb shell pidof com.example.hydrasample && echo "SURVIVED (unexpected)" || echo "KILLED (expected)"
adb logcat -d | grep -iE "DI-RT-RETPATCH|orchestrate:|CRITICAL|condemn|watchdog"
```

> Research / transparency only. Scoped to `com.example.hydrasample`; the module
> self-unloads (`DLCLOSE_MODULE_LIBRARY`) in every other process, so it is inert
> against any app but the sample.
