#!/system/bin/sh
# Attack 3 — KernelSU/Magisk module lib-replacement. Bind-mount a patched native
# lib over the protected app's extracted one, so the app loads the attacker's
# neutered library instead of the real one. INERT unless a payload is supplied:
# build one with make-payload.sh, which writes payload/libdicore.so.
#
# The protected runtime bakes its native code hash at build time, so a
# replaced/patched .so mismatches at runtime and the app self-terminates.
MODDIR=${0%/*}
PAYLOAD="$MODDIR/payload/libdicore.so"
TARGET_APP="com.example.hydrasample"

[ -f "$PAYLOAD" ] || { log -t hydra_neutralize "no payload — inert"; exit 0; }

for so in /data/app/*/${TARGET_APP}-*/lib/arm64/libdicore.so; do
    [ -f "$so" ] || continue
    mount -o bind "$PAYLOAD" "$so" \
        && log -t hydra_neutralize "bind-mounted patched libdicore.so over $so" \
        || log -t hydra_neutralize "bind-mount FAILED over $so"
done
