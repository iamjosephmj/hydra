# magisk-bindmount-lib (red-team — research only)

Magisk module that bind-mounts a patched `libdicore.so` over the target app's
extracted native lib, so the app loads the tampered lib from **load time** (deeper
than an in-memory patch — present before any snapshot).

1. `MODE=trip ./make-payload.sh <trail libdicore.so>` — flips one `.text` NOP so the
   bytes differ from the baked hash → certifies **G2** on-disk/build-hash catches an
   on-disk replacement. Or `MODE=retpatch FILEOFF=<hex> ./make-payload.sh <so>` to
   `ret`-patch a function on disk (e.g. `di_enforce_kill`) → certifies **B1** (the
   watchdog still kills with the in-process path neutered from load).
2. Package `module.prop`+`post-fs-data.sh`+`payload/` as a zip, install in Magisk,
   reboot. Inert until a payload exists.

`post-fs-data.sh` is the same bind-mount primitive as the public KSU demo, in Magisk
form for the raven rig.
