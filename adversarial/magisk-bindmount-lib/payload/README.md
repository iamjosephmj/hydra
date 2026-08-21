# payload/ — intentionally empty in git

This folder ships **empty on purpose**. The payload it holds —
`payload/libdicore.so` — is a *generated attack artifact*, not source, so it is
git-ignored (see `.gitignore`) and never committed.

Build it locally from a `libdicore.so` (run from the module dir, one level up):

```bash
MODE=trip ./make-payload.sh path/to/libdicore.so          # flip one .text NOP -> YIELD (fails G2's baked hash)
# or:
MODE=retpatch FILEOFF=0x<hex> ./make-payload.sh path/to/libdicore.so   # ret-patch a function on disk
```

Either mode writes `payload/libdicore.so`. The module is **inert until this file
exists** (`post-fs-data.sh` exits with "no payload — inert" if it's absent).

Why it isn't committed:
- it's a tampered, integrity-failing native binary — not something to ship in a repo;
- it's tied to one specific build of the lib, so a committed copy would be stale the
  moment the runtime is rebuilt. Generating from a current `libdicore.so` keeps it correct.
