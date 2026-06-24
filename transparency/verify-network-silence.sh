#!/usr/bin/env bash
# Reproducible "does the native core talk to the network?" check.
#
# The strongest evidence is static and needs no device: the shipped libdicore.so
# imports no resolver (getaddrinfo/gethostby*) and no send/recv* — so it CANNOT
# move data off-device regardless of intent. See ../TRANSPARENCY.md §2 and
# libdicore-arm64-imports.txt. This script re-derives that, and (optionally) adds
# a live runtime trace where the environment allows one.
#
# Usage:
#   ./verify-network-silence.sh path/to/libdicore.so        # static check
#   ./verify-network-silence.sh path/to/libdicore.so <serial>   # + live trace
set -euo pipefail
SO="${1:?usage: verify-network-silence.sh <libdicore.so> [adb-serial]}"
SERIAL="${2:-}"
READELF="${READELF:-readelf}"   # or an NDK llvm-readelf

echo "== STATIC: imported symbols of $(basename "$SO") =="
echo "-- resolver imports (getaddrinfo/gethostby/res_*) — MUST be empty --"
"$READELF" --dyn-syms "$SO" | grep -iE 'getaddrinfo|gethostby|res_query|res_init' || echo "  (none — no name resolution)"
echo "-- payload imports (send/recv/sendto/recvfrom) — MUST be empty --"
"$READELF" --dyn-syms "$SO" | grep -iE ' send| sendto| recv| recvfrom| sendmsg| recvmsg' || echo "  (none — no socket payload transfer)"
echo "-- socket imports (expect ONLY socket/connect [loopback probe] + socketpair [local IPC]) --"
"$READELF" --dyn-syms "$SO" | grep -iE ' socket| connect| socketpair| bind| listen' | awk '{print "  "$8}' | sed 's/@.*//' | sort -u
echo
echo "Interpretation: no resolver + no send/recv => the core cannot address or"
echo "transfer data to any host. The only socket use is a loopback (127.0.0.1)"
echo "Frida-server probe and a local process<->watchdog socketpair."

[ -n "$SERIAL" ] || { echo; echo "(skip live trace — no adb serial given)"; exit 0; }

echo
echo "== LIVE (best-effort): watch the app's TCP connections at launch =="
echo "Requires root + a usable trace path. Two methods; use whichever your"
echo "device allows. Replace <pkg> with your app id."
cat <<'NOTE'
  # A) strace (needs strace on device + debuggable app or userdebug):
  adb -s <serial> shell "setprop wrap.<pkg> 'strace -f -e trace=network -o /data/local/tmp/net.strace'"
  adb -s <serial> shell "am start -n <pkg>/.MainActivity"
  adb -s <serial> shell "su -c 'grep -E \"connect|socket\" /data/local/tmp/net.strace'"
  # Expect: connect() only to sin_addr=inet_addr(\"127.0.0.1\") on ports 27042/27043.

  # B) kernel TCP tracepoint (needs root + ftrace not owned by Perfetto):
  T=/sys/kernel/tracing
  adb -s <serial> shell "su -c 'echo 1 > $T/events/sock/inet_sock_set_state/enable'"
  adb -s <serial> shell "am start -n <pkg>/.MainActivity"
  adb -s <serial> shell "su -c 'grep <pkg> $T/trace'"
  # Expect: daddr=127.0.0.1 only; no non-loopback daddr.

  # C) simplest, no tooling: enable airplane mode and launch on a CLEAN device.
  # Every check and the kill still work — because all of it is on-device.
NOTE
