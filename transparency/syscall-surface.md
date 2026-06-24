# Syscall &amp; capability surface

The native core's *capability ceiling* — what it is able to do at the OS level.
Two layers bound it: the **raw syscalls** it issues directly (bypassing libc),
and the **libc functions** it imports (the complete list is in
[`libdicore-arm64-imports.txt`](libdicore-arm64-imports.txt)).

## Raw syscalls it issues directly (`syscalls.cpp`)

To read sensitive files without going through interposable libc wrappers, the
core issues a small, fixed set of raw syscalls — **all of them file I/O**:

| Raw wrapper | Syscall | Use |
|---|---|---|
| `raw_openat` | `openat` | open `/proc/self/...`, the installed APK |
| `raw_read_full` | `read` | read those files |
| `raw_lseek` | `lseek` | seek within them |
| `raw_fstat_size` | `fstat` | size them |
| `raw_mmap_readonly` | `mmap` | map the APK / snapshot pages (read-only) |
| `raw_munmap` | `munmap` | unmap them |
| `raw_close` | `close` | close fds |

**There is no raw network, no raw write-to-disk, no raw exec, no raw ptrace in
this layer** — it is read-only file access only.

## What it touches through libc (higher-level operations)

From the imported-symbol list, the notable capabilities are:

- **Process / threading:** `fork`, `pthread_*`, `waitpid`, `kill`, `prctl`,
  `sigaction` — the forked watchdog and the kill path.
- **Tamper response:** `ptrace` — used **only on its own process / forked child**
  to deliver the organic-looking kill; it does not trace other apps.
- **Memory:** `mmap`/`mprotect`/`munmap` — integrity-snapshot pages.
- **Introspection:** `dl_iterate_phdr`, `opendir`/`readdir` (over `/proc/self`),
  `__system_property_get`.
- **Network:** `socket` + `connect` — the **loopback** Frida probe only
  (`127.0.0.1`); `socketpair` — the **local** watchdog IPC (not a network
  socket). **No `getaddrinfo`/`gethostby*` (no resolver) and no
  `send`/`recv`/`sendto`/`recvfrom` (no payload transfer).**

The combination — read-only raw file access, self-only `ptrace`, and a network
surface with no resolver and no send/recv — is why the core **cannot move data
off the device**, regardless of intent.
