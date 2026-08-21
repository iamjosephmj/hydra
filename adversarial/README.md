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
| 5 | **Crash-survival** (`zygisk-survive-crash/`) | after detection fires, an in-process native attacker tries to *survive the kill*: catch the fault signals (`SIGSEGV`/`SIGBUS`/`SIGABRT`) and `siglongjmp` past the in-process detonate, `SIGKILL` the forked watchdog on a tight loop, and race `PR_SET_DUMPABLE(0)` to deny its `ptrace` attach | the layered kill still wins → **killed** (see "Beyond detection" below) |
| 6 | **seccomp `/proc` spoofing** (`zygisk-seccomp-spoof/`) | a `SECCOMP_RET_USER_NOTIF` filter + root supervisor intercepts the `openat` reads of `/proc/self/{maps,status,mounts}` at the kernel boundary — below libc **and** raw `SVC` — and hands back a scrubbed copy, hiding injected libs / hooks from the RASP's introspection | the loader's in-memory view (`dl_iterate_phdr`) can't be spoofed and disagrees with the doctored maps → **killed** (see "Beyond hooking" below) |

### Deeper red-team modules (same attacks, deeper variants)

Beyond the entry-level probes above, the full red-team suite is included — each module
is scoped to `com.example.hydrasample` and inert against any other app, and each
targets an attack the shipped runtime withstands:

| Module | Class | Exercises |
|---|---|---|
| [`zygisk-ret-patch/`](zygisk-ret-patch/) | native, in-memory | `ret`-patch any internal `libdicore.so` function by offset — the generic vehicle for neutering the sweep entry (`nat_o`), the funnel (`collect`), or the kill path (`di_enforce_kill` / `di_condemn` / `di_detonate`). Beaten by the prologue self-hook watch + the forked watchdog. |
| [`magisk-bindmount-lib/`](magisk-bindmount-lib/) | on-disk, load-time | bind-mount a patched `libdicore.so` over the app's extracted lib (Magisk form of attack #3). Beaten by the build-baked `.text` hash. |
| [`lsposed-neutralize/`](lsposed-neutralize/) | Java/ART boundary | LSPosed hooks on the runtime's own up-calls — `FrameworkShim.q` (starve the fingerprint asset) and `K.o()` (skip the whole sweep). Beaten by native fingerprint recovery + the watchdog's completion deadline. |
| [`zygisk-survive-crash/`](zygisk-survive-crash/) | kill suppression | all-out attack on the kill itself (see attack #5). |
| [`zygisk-seccomp-spoof/`](zygisk-seccomp-spoof/) | observation spoofing | app-side seccomp filter (see attack #6); the generic root supervisor is documented in full but not shipped as a drop-in binary — see that module's README. |

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

**Transparency note, honestly stated.** The generic attack *techniques* AND the
runnable modules are published here — including `zygisk-survive-crash/`, the all-out
attack on the kill itself. The rule is timing, not secrecy: an attack is published
only once the shipped runtime is confirmed to **survive** it. When a red-team run finds
a surviving layer, it is fixed first, then the (now-defeated) attack is published. The
one thing deliberately withheld is any tool that is a **general-purpose evasion
primitive** whose usefulness is independent of this runtime — see the seccomp note
below. Every module here is scoped to the sample and inert against any other app.

## Beyond hooking: spoofing what the RASP *sees*

A newer bypass class doesn't hook or patch anything — it changes what the RASP
*observes*. Android's own seccomp-bpf (the same kernel mechanism Chrome's sandbox
uses) can be turned against the app: a `SECCOMP_RET_USER_NOTIF` filter traps the
`openat` syscalls the RASP uses to read `/proc/self/maps`, `mounts`, and `status`,
and a supervisor hands back a **scrubbed** copy. It intercepts at the kernel/syscall
boundary — **below libc and below raw `SVC #0`** — so a RASP built on raw syscalls to
dodge Frida gains nothing. It blinds every `/proc/self/maps`-based check
(loaded-library inventory, injected-hook classification, mount/root detection)
without touching a single byte of code.

Two things defeat it here:

- **An un-spoofable second view.** `dl_iterate_phdr` walks the dynamic linker's
  *in-memory* `link_map` — it makes **no syscall**, so a seccomp filter cannot touch
  it. The runtime cross-checks that authoritative view against `/proc/self/maps`: a
  loaded library the maps view hides, or an executable `.so` mapping the loader never
  placed, is a contradiction only a spoof can produce → detected.
- **A syscall-free kill.** The termination path is a wild-memory write, not a
  syscall, so even a perfect syscall interceptor can't filter the crash once a
  contradiction is found.

The generic technique is public and described here in full, and the **app-side**
filter module ships (`zygisk-seccomp-spoof/module.cpp`). The one piece held back is the
root **supervisor daemon** — a working `/proc`-hider is a general-purpose evasion
primitive that blinds *any* `/proc`-based RASP, not just this one, so shipping a
turnkey binary would hand out a live weapon unrelated to hydra. Its full algorithm is
documented step-by-step in `zygisk-seccomp-spoof/README.md` — the method is withheld
from being drop-in, not from being understood. The runtime defeats the attack (which
it now does) via the un-spoofable `dl_iterate_phdr` cross-check.

> Red-team / research use only. These modules tamper with a process on purpose;
> they are inert against any app other than the sample.
