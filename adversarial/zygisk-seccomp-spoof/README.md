# zygisk-seccomp-spoof (app-side filter + supervisor writeup)

The newest bypass class: don't hook or patch anything — change what the RASP
**observes**. A `SECCOMP_RET_USER_NOTIF` filter traps the `openat`/`openat2`
syscalls the runtime uses to read `/proc/self/{maps,status,mounts}`, and a
privileged supervisor hands back a **doctored** copy. It intercepts at the
kernel/syscall boundary — **below libc and below a raw `SVC #0`** — so a RASP built
on raw syscalls to dodge Frida gains nothing. Done right it blinds every
`/proc/self/maps`-based check (loaded-library inventory, injected-hook
classification, mount/root detection) without touching a byte of code.

**Result against hydra:** defeated. The runtime's G10 `proc_consistency` core
cross-checks the loader's *in-memory* view (`dl_iterate_phdr`, which makes **no
syscall** and cannot be trapped by a seccomp filter) against `/proc/self/maps`. A
library the maps view hides, or an executable `.so` mapping the loader never placed,
is a contradiction only a spoof can produce → detected → killed by the syscall-free
`di_detonate` (which the interceptor also can't filter).

## What ships here

- **`module.cpp`** — the app-side Zygisk module. In `postAppSpecialize` for the
  target it installs the `SECCOMP_RET_USER_NOTIF` filter on `openat`/`openat2` and
  **leaks the listener fd** for an external supervisor to adopt. On its own this does
  nothing malicious: with no supervisor servicing the notifications the trapped opens
  simply block.
- **`build.sh`, `module.prop`, `zygisk.hpp`** — build/packaging.

## What does NOT ship (by design): the supervisor daemon

The root supervisor that actually rewrites `/proc` is a **generic, works-against-any-RASP
`/proc`-hider** — a live evasion primitive whose usefulness is independent of whether
*this* runtime catches it. Consistent with this repo's policy (see `../README.md`), the
runnable daemon is not published. It is described here in full instead:

1. **Adopt the leaked fd.** Find the target pid (scan `/proc/<pid>/cmdline`), find the
   seccomp-notify fd in `/proc/<pid>/fd` (the symlink resolves to `anon_inode:seccomp
   notify`), then `pidfd_open(pid)` + `pidfd_getfd(pidfd, targetfd)` to duplicate that
   listener into the (root) supervisor. Root is required — `pidfd_getfd` and reading
   another process's `/proc/<pid>/mem` are privileged.
2. **Receive each trap.** `ioctl(nfd, SECCOMP_IOCTL_NOTIF_RECV, &req)` blocks until the
   app calls `openat`. The pathname pointer is in `req.data.args[1]` (arg 1 of
   `openat`), which the supervisor reads out of the app with `pread(/proc/<pid>/mem,
   …, args[1])`.
3. **Adjudicate.** If the path isn't one of `/proc/self/{maps,status,mounts,mountinfo}`,
   respond with `flags = SECCOMP_USER_NOTIF_FLAG_CONTINUE` — the kernel lets the real
   syscall run untouched. If it *is*, build a doctored copy.
4. **Spoof via fd substitution.** Open the real `/proc/<pid>/maps`, stream it into a
   `memfd`, inserting/removing lines (e.g. hiding an injected `.so`, or the demo's
   fake `…/libEVIL_SPOOF.so r-xp` line), then `SECCOMP_IOCTL_NOTIF_ADDFD` that memfd
   into the target and return its fd number as the syscall result. The app now reads
   the attacker's file where it asked for `/proc/self/maps`. `SECCOMP_IOCTL_NOTIF_SEND`
   completes the trap.

That is the entire technique. Reproducing it is a modest amount of code against the
public `seccomp` uapi — the point of withholding the drop-in binary is not secrecy of
the method but not shipping a turnkey `/proc`-hider usable against arbitrary apps.

## Observe the defense

Against the hydra sample, applying the app-side filter + a supervisor spoof produces a
`CRITICAL proc_consistency/proc_maps_injected` in the runtime trail (the fabricated
`.so` mapping is absent from the loader's `link_map`), and the process is killed —
demonstrating the un-spoofable second view.

> Research / transparency only. Scoped to `com.example.hydrasample`; inert elsewhere.
