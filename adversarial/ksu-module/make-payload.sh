#!/usr/bin/env bash
# Build the patched libdicore.so payload for the lib-replacement module (attack 3).
# Extracts libdicore.so from the hydra sample APK and flips ONE .text NOP
# (0xD503201F) to a YIELD hint (0xD503203F) — a functional no-op, so the lib loads
# and runs identically, but its code bytes no longer match the build-baked hash.
# The module bind-mounts this over the app's lib; the runtime should then trip and
# self-terminate.
#
# Usage: bash make-payload.sh [path/to/app.apk]
set -eu
HERE="$(cd "$(dirname "$0")" && pwd)"
APK=${1:-$HERE/../../sample/build/outputs/apk/release/sample-release.apk}
ABI=arm64-v8a
mkdir -p "$HERE/payload"; OUT="$HERE/payload/libdicore.so"

tmp=$(mktemp -d)
unzip -o -q "$APK" "lib/$ABI/libdicore.so" -d "$tmp"
LIB="$tmp/lib/$ABI/libdicore.so"
[ -f "$LIB" ] || { echo "no libdicore.so in $APK" >&2; exit 1; }

python3 - "$LIB" "$OUT" <<'PY'
import sys
lib, out = sys.argv[1], sys.argv[2]
d = bytearray(open(lib, "rb").read())
NOP = bytes.fromhex("1f2003d5")
i = 0x60000
while i < len(d):
    j = d.find(NOP, i)
    if j < 0: raise SystemExit("no aligned NOP found in .text to patch")
    if j % 4 == 0:
        d[j] = 0x3f            # NOP -> YIELD (still a harmless hint)
        print(f"patched .text NOP@0x{j:x} (NOP->YIELD) -> {out} ({len(d)} bytes)"); break
    i = j + 1
open(out, "wb").write(d)
PY
rm -rf "$tmp"
