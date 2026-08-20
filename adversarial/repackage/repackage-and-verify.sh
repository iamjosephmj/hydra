#!/usr/bin/env bash
# Attack 1 — REPACKAGE. The realistic no-root delivery: patch one instruction in
# the shipped native lib, re-zip, and re-sign with the attacker's OWN key, then
# ship the resulting APK. A hydra-protected app must notice that (a) its native
# code no longer matches what was baked at build time and (b) it is signed by a
# different key — and self-terminate.
#
# The patch flips one `.text` NOP (0xD503201F) to a YIELD hint (0xD503203F): a
# functional no-op so the lib still loads, but its code bytes have changed.
#
# Usage:  JAVA_HOME=<jdk> bash repackage-and-verify.sh [in.apk] [out.apk] [adb-serial]
set -eu
HERE="$(cd "$(dirname "$0")" && pwd)"
IN=${1:-$HERE/../../sample/build/outputs/apk/release/sample-release.apk}
OUT=${2:-/tmp/hydra-sample-repackaged.apk}
SERIAL=${3:-}
PKG=com.example.hydrasample
ABI_SO=lib/arm64-v8a/libdicore.so
BT=$(ls -d "$HOME"/Android/Sdk/build-tools/* | sort -V | tail -1)
ADB="adb ${SERIAL:+-s $SERIAL}"

command -v java >/dev/null || { echo "java not on PATH (set JAVA_HOME/PATH)"; exit 1; }
[ -f "$IN" ] || { echo "input APK not found: $IN (build the sample first: ./gradlew :sample:assembleRelease)"; exit 1; }

W=$(mktemp -d); cp "$IN" "$W/base.apk"
( cd "$W"
  unzip -q base.apk "$ABI_SO"
  python3 - <<PY
d=bytearray(open("$ABI_SO","rb").read())
NOP=bytes.fromhex("1f2003d5")                 # little-endian 0xD503201F
i=0x60000                                      # deep inside .text, past the entry stubs
while i < len(d):
    j=d.find(NOP,i)
    if j<0: raise SystemExit("no NOP found to patch")
    if j%4==0: d[j]=0x3f; print(f"patched .text NOP@0x{j:x} (NOP->YIELD)"); break
    i=j+1
open("$ABI_SO","wb").write(d)
PY
  zip -q -0 -X base.apk "$ABI_SO"              # store uncompressed so it stays mmap-able
  "$BT/zipalign" -f -P 16 4 base.apk aligned.apk
  # attacker re-signs with their OWN fresh key (NOT the original signer)
  keytool -genkeypair -keystore atk.jks -storepass atk -keypass atk -alias a \
    -keyalg RSA -keysize 2048 -validity 3650 -dname "CN=Repackager, O=Attacker, C=XX" >/dev/null 2>&1
  "$BT/apksigner" sign --ks atk.jks --ks-pass pass:atk --key-pass pass:atk aligned.apk
)
cp "$W/aligned.apk" "$OUT"; rm -rf "$W"
echo "repackaged (patched-lib, attacker-signed) APK -> $OUT"

[ -n "$($ADB devices | awk 'NR>1&&$2=="device"')" ] || { echo "(no device — install + launch skipped)"; exit 0; }
echo "== installing the repackaged APK =="
$ADB uninstall "$PKG" >/dev/null 2>&1 || true
$ADB install "$OUT"
$ADB shell am start -n "$PKG/.MainActivity" >/dev/null 2>&1 || true
sleep 5
if $ADB shell pidof "$PKG" >/dev/null 2>&1; then
  echo "RESULT: process SURVIVED — protection did NOT trip (unexpected on a clean device)"
else
  echo "RESULT: process KILLED — the protected app rejected the repackaged build (expected)"
fi
