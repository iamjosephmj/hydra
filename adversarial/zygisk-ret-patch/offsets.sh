#!/usr/bin/env bash
# Recover the runtime offsets (symbol vaddr) of the neutralization targets from an
# UNSTRIPPED trail build of libdicore.so (externalNativeBuildDebug output). Simulates
# an attacker who reverse-engineered the shipped lib. The SAME .so must be installed
# on-device; feed an offset to hydra.rt.offset. Base+offset = runtime addr.
set -eu
SO="${1:?usage: offsets.sh <unstripped libdicore.so>}"
NM="${NM:-$(ls "$HOME"/Android/Sdk/ndk/*/toolchains/llvm/prebuilt/*/bin/llvm-nm | tail -1)}"
FILT="${FILT:-$(ls "$HOME"/Android/Sdk/ndk/*/toolchains/llvm/prebuilt/*/bin/llvm-cxxfilt | tail -1)}"
echo "# offset(hex)  symbol   — setprop hydra.rt.offset 0x<offset>; hydra.rt.label <name>"
"$NM" --defined-only "$SO" 2>/dev/null | while read -r val typ sym; do
  d=$("$FILT" "$sym" 2>/dev/null || echo "$sym")
  case "$d" in
    *di_enforce_kill*|*di_condemn*|*di_detonate*|*"::collect("*|*nat_o*|*watchdog_request_kill*|*self_detonate*|*beat_once*)
      echo "0x$val  $d" ;;
  esac
done
