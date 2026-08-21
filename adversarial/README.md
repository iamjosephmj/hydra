# Adversarial suite — can hydra survive a real attacker?

hydra bundles an **enforcement** device-integrity runtime: when it detects tampering
it **self-terminates the protected process**. This suite reproduces the four attack
classes a determined attacker actually uses against an on-device RASP, run against
the **hydra sample app** (`com.example.hydrasample`). For each, the expected result
is the same — the protected app **does not survive launch**.

| # | Attack | What it does | Expected result |
|---|---|---|---|
| 1 | **Repackage** (`repackage/`) | patch one `.text` instruction in the shipped native lib, re-zip, re-sign with the attacker's key — the no-root "ship a neutered APK" delivery | app detects the modified lib + foreign signer → **killed** |
| 2 | **Zygisk** (`zygisk-probe/`) | a native Zygisk module patches the protected lib's `.text` **in memory** at process start (the deepest on-device tamper) | live-code-hash mismatch → **killed** |
| 3 | **KernelSU module** (`ksu-module/`) | a KSU/Magisk module **bind-mounts** a patched native lib over the app's on-disk one | replaced lib fails self-integrity → **killed** |
| 4 | **LSPosed** (`lsposed-module/`) | an in-process ART method hook at Zygote fork — no frida artifacts | ART hook detected → **killed** |
| 5 | **Crash-survival** (kill-path stress test) | after detection fires, an in-process native attacker tries to *survive the kill*: catch the fault signals (`SIGSEGV`/`SIGBUS`/`SIGABRT`) and `siglongjmp` past the in-process detonate, `SIGKILL` the forked watchdog on a tight loop, and race `PR_SET_DUMPABLE(0)` to deny its `ptrace` attach | the layered kill still wins → **killed** (see "Beyond detection" below) |

## How to observe the result

After applying an attack and launching the app, the process should **not stay
alive**:

```bash
adb shell am start -n com.example.hydrasample/.MainActivity
sleep 4
adb shell pidof com.example.hydrasample && echo "SURVIVED (unexpected)" || echo "KILLED (expected)"
```

## Important: test on a device whose *only* compromise is the attack

The runtime kills on **any** integrity failure, so a device that is already rooted /
hooked will kill the genuine app too — you can't attribute the kill to the attack.
Run each vector on a **clean, unlocked test device** (or emulator) where the attack
you are applying is the *only* modification, then compare against the genuine app
running normally on the same device. Attacks 2–4 need root + the respective
framework (Zygisk / KernelSU / LSPosed) installed to load at all.

## Beyond detection: can the kill itself be suppressed?

Detecting tampering is only half the job — a serious attacker who already has code
in the process will try to stop the **kill** rather than hide the tamper. So the
runtime is red-teamed against *kill-suppression*, not just detection (attack #5
above). The kill is deliberately layered so that no single in-process hook defeats it:

- the in-process crash is a **syscall-free wild-memory write** — a caught `SIGSEGV`
  just re-faults on the next instruction, and `SIGKILL` is uncatchable;
- the real executioner is a **separate forked watchdog process** — its code cannot
  be hooked from inside the app (post-`fork` copy-on-write), it **re-forks** if
  killed, and it crashes the app *from the outside* via `ptrace`;
- the kill decision is **decoupled from the hookable in-process kill function**: the
  watchdog acts on an authenticated heartbeat verdict, so `ret`-hooking the
  in-process path does not save the process.

**Transparency note, honestly stated.** The generic attack *techniques* are described
here in full. We do **not** ship the runnable "survive the kill" module in this public
repo: a module that actually suppressed a layer would be a working bypass, and we
publish an attack here only once the shipped runtime is confirmed to survive it. When
a red-team run finds a surviving layer, it is fixed first, then the (now-defeated)
attack is published. This is why the offensive depth lives in a private suite and the
public one shows only attacks the runtime withstands — not to hide behavior, but to
avoid handing out a live neutralization while a fix is in flight.

> Red-team / research use only. These modules tamper with a process on purpose;
> they are inert against any app other than the sample.
