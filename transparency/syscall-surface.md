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
  (`127.0.0.1`); `socketpair` — the **local** watchdog IPC (an AF_UNIX socket,
  not a network socket). `sendto`/`recvfrom` are present but used **only on that
  AF_UNIX socketpair** (the process↔watchdog heartbeat; bionic routes `send`/`recv`
  through them). **No `getaddrinfo`/`gethostby*`/`res_*` (no resolver), and
  `connect` only ever targets `127.0.0.1`** — so the core cannot address a remote
  host, and its `sendto`/`recvfrom` are confined to a kernel-local socket pair.

The combination — read-only raw file access, self-only `ptrace`, no resolver, and
socket payload calls confined to a local AF_UNIX pair with loopback-only `connect`
— is why the core **cannot move data off the device**, regardless of intent.
