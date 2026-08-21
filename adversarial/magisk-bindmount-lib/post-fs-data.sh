#!/system/bin/sh
# Bind-mount payload/libdicore.so over the target app's extracted native lib, so the
# app loads the attacker's lib from load time (before any in-memory snapshot). Tests
# G2 on-disk/build-hash integrity AND, if the payload ret-patches the kill path,
# whether B1's watchdog still kills. Inert without a payload (build with make-payload.sh).
MODDIR=${0%/*}
PAYLOAD="$MODDIR/payload/libdicore.so"
APP="com.example.hydrasample"
[ -f "$PAYLOAD" ] || { log -t di_rt "no payload — inert"; exit 0; }
for so in /data/app/*/${APP}-*/lib/arm64/libdicore.so; do
    [ -f "$so" ] || continue
    mount -o bind "$PAYLOAD" "$so" && log -t di_rt "bind-mounted over $so" || log -t di_rt "bind FAILED $so"
done
