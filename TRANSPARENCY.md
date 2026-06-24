# Transparency &amp; Verifiability

You should not have to *trust* a closed native library that runs in your users'
processes. You should be able to **verify** what it can and cannot do. This page
is about verification — concrete commands you can run yourself, not promises.

It is also honest about the limits (see [the bottom](#honest-limitations)). hydra
raises the *cost and time* of reverse-engineering your app; it does not make a
client-side app unbreakable, and nothing here claims otherwise.

---

## TL;DR — what you can confirm in about a minute

1. **hydra adds no `INTERNET` permission.** The runtime *cannot* open a network
   connection on its own — verifiable in one command.
2. **The only network-shaped syscall in the whole library is a loopback probe to
   `127.0.0.1`** (to detect a local Frida server). It sends no data, never
   contacts a non-loopback address, and can't even run unless *your* app already
   holds `INTERNET`.
3. **Everything works with the network off.** Every check and the kill decision
   are computed on-device; turn networking off and the protection is unaffected.
4. **It adds exactly one permission: `QUERY_ALL_PACKAGES`** (used on-device for
   clone/root-manager detection) — and you can strip it if you can't justify it.

The fear "a closed lib I can't inspect might be the thing that leaks my users'
data" is reasonable. The answer below is: it has **no network capability of its
own**, and you can prove that in seconds.

---

## 1. It cannot exfiltrate: no `INTERNET` permission

On Android, creating an internet socket (`AF_INET`/`AF_INET6`) requires the
`android.permission.INTERNET` permission — it gates membership in the `inet`
(AID_INET, gid 3003) group, and without it `socket()` fails with `EACCES`. The
hydra runtime declares **no `INTERNET` permission**, so on its own it has **no
ability to transmit anything anywhere.**

**Verify it.** Build your app (or the bundled sample), then:

```bash
# Exactly one permission is contributed by hydra — and it is NOT internet.
unzip -p <your-app>.apk AndroidManifest.xml > /dev/null   # (APK manifest is binary)
$ANDROID_HOME/build-tools/<ver>/aapt dump permissions <your-app>-release.apk
# → you will see android.permission.QUERY_ALL_PACKAGES
# → you will NOT see android.permission.INTERNET contributed by hydra
```

The cleanest proof is a **before/after diff** of your own merged manifest:

```bash
# Build once without the hydra plugin, once with it, and diff the permissions.
# The only addition is QUERY_ALL_PACKAGES. INTERNET is never added.
diff <(aapt dump permissions app-without-hydra.apk | sort) \
     <(aapt dump permissions app-with-hydra.apk    | sort)
```

> If your *own* app declares `INTERNET` (most apps do), it will show up — but
> that is **your** permission, not hydra's. hydra contributes none.

---

## 2. The only network-shaped call is a loopback Frida probe

hydra's anti-Frida check tries to connect to the default `frida-server` ports on
**localhost** and treats a successful connect as "a Frida server is running
here." That is the entire extent of anything socket-related in the library. The
source is small and unambiguous:

```c
// Loopback connect to a default frida-server port. true only on a completed
// connect (something is listening). Fail-open on any socket error.
bool frida_port_listening(int port) {
    int s = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
    if (s < 0) return false;                 // no INTERNET perm -> can't even probe
    sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons((uint16_t)port);
    addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);   // 127.0.0.1 — never a remote host
    bool listening = (connect(s, (sockaddr*)&addr, sizeof(addr)) == 0);
    close(s);                                 // no send(), no recv() — no data ever
    return listening;
}
```

What this means, precisely:

- It only ever targets **`127.0.0.1`** (`INADDR_LOOPBACK`). It never connects to
  any external address.
- It **sends and receives nothing** — it opens, connects, and closes. The only
  bit of information it derives is "did the connect succeed," locally.
- If your app has **no `INTERNET` permission**, `socket()` returns `< 0` and the
  probe simply no-ops (fail-open). hydra's network reach is therefore **bounded
  by your app's own permissions**, and even at the maximum it is **loopback
  only**.

**Verify it against the actual binary.** The native core's *imported* symbols
tell the whole story — and you can read them straight out of the shipped `.so`
(this is the real, current obfuscated `libdicore.so`):

```bash
# (NDK ships llvm-readelf; plain readelf works too)
readelf --dyn-syms lib*.so | grep -iE 'socket|connect|send|recv|getaddrinfo|gethostby|res_'
```

What you will find, and what it means:

| Imported? | Symbol(s) | Why it's there |
|:--:|---|---|
| ✅ yes | `socket`, `connect` | the **loopback** Frida probe above — `127.0.0.1` only |
| ✅ yes | `socketpair` | the **local** watchdog IPC (process ↔ its forked child) — not a network socket |
| ❌ no | `getaddrinfo`, `gethostbyname`, `res_*` | **no name resolution** — it cannot even *resolve* a remote host |
| ❌ no | `send`, `sendto`, `recv`, `recvfrom` | **no socket payload transfer** — it never sends or receives data over a socket |

So the binary itself confirms it: the only socket calls exist for a loopback
probe and local IPC, and with **no resolver and no send/recv**, there is no way
for it to address or move data to anywhere off the device.

---

## 3. Verify it dynamically — watch it stay silent

Static claims are good; watching it run is better.

**Easiest — run with the network off.** Put the device in airplane mode (or
revoke the app's network access) and launch. The app starts, every hydra check
runs, and the kill-on-tamper behavior is unchanged — because **all of it is
on-device.** A library that needed to phone home would degrade or fail here. It
doesn't.

**Trace the syscalls.** On a debuggable build / rooted test device:

```bash
adb shell strace -f -e trace=network -p $(adb shell pidof <your.app.id>)
# You will see no connect() to any address other than 127.0.0.1, and no
# sendto()/sendmsg() carrying data off-device.
```

**Capture the wire.** Point the device at a logging proxy (mitmproxy) or run
`tcpdump`, then exercise the app. There is no traffic attributable to hydra,
because it has nowhere to send it.

---

## 4. Exactly what it reads (and what it never does)

For completeness, here is everything the runtime touches on-device. All of it
stays local; none of it is transmitted (it can't be — see §1).

**Reads / inspects:**

- `/proc/self/status` — the `TracerPid` field (is a debugger attached?).
- `/proc/self/task/*/comm` — thread names (Frida worker-thread fingerprints).
- `/proc/self/maps` — mapped regions (injected hook frameworks, RWX code caches).
- Loaded libraries via `dl_iterate_phdr` (injected-library / GOT / .text checks).
- The **installed app's own APK + splits** (its `sourceDir`) — to read hydra's
  baked integrity assets.
- The **installed package list** (`QUERY_ALL_PACKAGES`) — clone/virtual-space and
  root/attestation-spoofer-manager detection.

**Does:**

- Forks a small watchdog child and uses `ptrace` **on that child / its own
  process only** to deliver the kill. It does not trace other apps.
- `mmap`/`mprotect` for its own integrity-snapshot pages.

**Never:**

- No `INTERNET` socket of its own, no remote connect, no DNS, no data sent.
- No persistent storage of anything off-device, no analytics, no advertising ID,
  no device fingerprint sent anywhere (there is no "anywhere").

---

## 5. The one permission it adds: `QUERY_ALL_PACKAGES`

hydra contributes a single permission, used **only on-device** to enumerate
installed packages for clone-space and root/spoofer-manager detection. The result
feeds the local kill decision and is never transmitted.

If you ship on Google Play, `QUERY_ALL_PACKAGES` is a *sensitive* permission and
must be declared in the Play Console under an allowed use case (anti-fraud /
security applies). If you can't or don't want to use it, strip it from the merged
manifest with `tools:node="remove"` and add a fixed `<queries>` list instead — at
the cost of reduced clone/root-manager coverage.

---

## 6. Inspectable artifacts (in this repo)

Committed evidence you can read and re-derive yourself, under
[`transparency/`](transparency/):

- **[`libdicore-arm64-imports.txt`](transparency/libdicore-arm64-imports.txt)** —
  the complete list of external functions the shipped arm64 core calls, dumped
  from the real `libdicore.so` with `llvm-readelf`. This is the ground truth for
  §2: no resolver, no `send`/`recv`, only loopback `socket`/`connect` +
  `socketpair`.
- **[`SBOM.md`](transparency/SBOM.md)** — the software bill of materials: your
  logic plus exactly two vendored libraries (mbed TLS 3.6.2, miniz 3.0.2), with
  licenses. No analytics, networking, crash-reporting, or identifier library.
- **[`syscall-surface.md`](transparency/syscall-surface.md)** — the capability
  ceiling: the raw syscalls it issues (file I/O only) and what it reaches through
  libc (self-only `ptrace`, loopback-only sockets).
- **[`verify-network-silence.sh`](transparency/verify-network-silence.sh)** — a
  script that re-derives the static network proof from any `libdicore.so`, plus
  the device commands to watch it live where your environment permits a trace.

> Note on live syscall traces: capturing one depends on the device (a `strace`
> binary, a debuggable build, or an ftrace path not owned by Perfetto, plus a
> *clean* device where the app isn't killed on launch). The **static import
> proof above does not depend on any of that** — it shows the capability is
> absent in the binary, which is strictly stronger than a trace that merely
> fails to exercise it.

---

## Honest limitations

- **The shipped `libdicore.so` is closed and obfuscated today.** This page proves
  what it *can't do* (reach the network) and documents what it *does* read, but it
  does not yet let you read the implementation line-by-line.
- **Client-side protection is not unbreakable.** A determined, well-resourced
  reverse engineer will defeat any on-device protection given time. hydra's value
  is *delay, not denial* — it raises the cost from "hours" to "meaningfully more,"
  and it stops automated/script-driven repackaging. If your threat model needs a
  guarantee, the answer is server-side, not the client.
- **Kill-by-default affects QA.** Emulators and rooted/hooked test devices are
  treated as compromised and the process is terminated — test on real, clean
  devices.

### On the roadmap (not done yet — listed honestly)

- **Reproducible builds + signed releases:** so you can confirm the shipped
  binary is built from exactly the published source (no per-release surprise).
- **A readable reference build:** the same logic compiled *without* obfuscation,
  published for inspection alongside the hardened production build.

If either of these would change your decision to adopt, open an issue and say so —
it helps prioritize.
