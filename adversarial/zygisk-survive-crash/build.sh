#!/usr/bin/env bash
# Build the survive-crash native Zygisk module and package it as a Magisk/Zygisk
# module zip. Requires NDK r27+. red-team tooling (research / transparency only).
set -eu
HERE="$(cd "$(dirname "$0")" && pwd)"
NDK=${NDK:-$(ls -d "$HOME"/Android/Sdk/ndk/* | sort -V | tail -1)}
TC="$NDK/toolchains/llvm/prebuilt/linux-x86_64"
API=${API:-28}
OUT="$HERE/build"; rm -rf "$OUT"; mkdir -p "$OUT/module/zygisk"
for ABI in arm64-v8a:aarch64-linux-android x86_64:x86_64-linux-android; do
  A="${ABI%%:*}"; TRIPLE="${ABI##*:}"
  "$TC/bin/${TRIPLE}${API}-clang++" \
      -std=c++17 -O2 -fPIC -shared -fvisibility=hidden -Wall \
      -ffunction-sections -fdata-sections -Wl,--gc-sections -static-libstdc++ \
      -o "$OUT/module/zygisk/$A.so" "$HERE/module.cpp" -llog
done
cp "$HERE/module.prop" "$OUT/module/"
( cd "$OUT/module" && zip -qr "$OUT/di-rt-survive-crash.zip" . )
echo "packaged: $OUT/di-rt-survive-crash.zip"
