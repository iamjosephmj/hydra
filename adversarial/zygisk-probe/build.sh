#!/usr/bin/env bash
# Build the hydra-zygisk-probe native module and package it as a Magisk/KernelSU
# Zygisk module zip. Requires NDK r27+.
set -eu
HERE="$(cd "$(dirname "$0")" && pwd)"
NDK=${NDK:-$(ls -d "$HOME"/Android/Sdk/ndk/* | sort -V | tail -1)}
TC="$NDK/toolchains/llvm/prebuilt/linux-x86_64"
API=${API:-28}
OUT="$HERE/build"; rm -rf "$OUT"; mkdir -p "$OUT/module/zygisk"
"$TC/bin/aarch64-linux-android${API}-clang++" \
    -std=c++17 -O2 -fPIC -shared -fvisibility=hidden -Wall \
    -ffunction-sections -fdata-sections -Wl,--gc-sections -static-libstdc++ \
    -o "$OUT/module/zygisk/arm64-v8a.so" "$HERE/module.cpp" -llog
cp "$HERE/module.prop" "$OUT/module/"
( cd "$OUT/module" && zip -qr "$OUT/hydra-zygisk-probe.zip" . )
echo "packaged: $OUT/hydra-zygisk-probe.zip"
