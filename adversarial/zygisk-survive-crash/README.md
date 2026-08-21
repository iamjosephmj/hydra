# zygisk-survive-crash (red-team — research only)

The deep stress test for the **kill mechanism**. The shallow probe patches `.text`
and asks "is it detected?"; this one lets detection happen and asks **"can the kill
be survived?"** — it attacks every enforcement layer at once and logs whether the
process stays alive.

## What it attacks

| Layer | Enforcement | This module's move |
|---|---|---|
| in-process detonate | `di_detonate` wild-write → SIGSEGV | catch SIGSEGV/BUS/ABRT/ILL/FPE on an alt-stack, `siglongjmp` back to a survive loop |
| external watchdog | forked child `PTRACE_ATTACH`es + corrupts PC/SP | scan `/proc` for our child pid, `SIGKILL` it on a 1 ms loop |
| organic kill precondition | `PR_SET_DUMPABLE(1)` set at kill time | race `PR_SET_DUMPABLE(0)` to deny the attach (EPERM) |
| escalation / B1 | SIGKILL backstop + watchdog verdict | (unbeatable by design — this measures how close we get) |

Modes: `adb shell setprop hydra.rt.mode <catch-signals|kill-watchdog|deny-ptrace|full>`
(default `full`).

## Build + install (on the clean raven rig — see a clean rooted test device (see ../README.md))

```bash
./build.sh                       # -> build/di-rt-survive-crash.zip (arm64 + x86_64)
adb push build/di-rt-survive-crash.zip /sdcard/Download/
# Magisk -> Modules -> Install from storage -> select the zip -> reboot
```

## Run + observe (Track B — suppression resistance)

Use the **trail build** of the sample (`DICORE_*_ENFORCE=0`) so the trail shows the
whole kill flow, then apply this module on top and watch whether the kill path still
completes and whether the process outlives the verdict:

```bash
adb logcat -c
adb shell setprop hydra.rt.mode full
adb shell am start -n com.example.hydrasample/.MainActivity
sleep 6
adb shell pidof com.example.hydrasample && echo "SURVIVED (unexpected — a layer was defeated)" || echo "KILLED (expected)"
adb logcat -d | grep -iE "DI-RT-SURVIVE|orchestrate:|CRITICAL|condemn|watchdog"
```

Read the trail:
- `SIGKILLed N watchdog child(ren)` **followed by the child re-appearing / the kill
  still landing** → the re-forking watchdog + B1 held.
- `STILL ALIVE tick=…` lines that STOP shortly after the verdict → the kill won.
- `STILL ALIVE` lines that CONTINUE past the verdict → **a layer was defeated →
  record it in the results table in ../README.md and harden before any public claim.**

## Certification meaning

If the process cannot be kept alive under `full`, the layered kill is certified
suppression-resistant against an in-process native attacker. Any mode that DOES
survive is a private finding that must be fixed before the public hydra writeup can
claim survival for this attack class.
