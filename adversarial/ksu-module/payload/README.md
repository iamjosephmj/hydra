# payload/ — intentionally empty in git

This folder ships **empty on purpose**. The payload it holds —
`payload/libdicore.so` — is a *generated attack artifact*, not source, so it is
git-ignored (see `.gitignore`) and never committed.

Build it locally:

```bash
bash ../make-payload.sh            # extracts libdicore.so from the sample APK,
                                   # flips one .text NOP -> YIELD, writes payload/libdicore.so
```

The module is **inert until this file exists** (`post-fs-data.sh` exits with
"no payload — inert" if it's absent).

Why it isn't committed:
- it's a tampered, integrity-failing native binary — not something to ship in a repo;
- it's tied to one specific build of the lib, so a committed copy would be stale the
  moment the runtime is rebuilt. Generating from the current APK keeps it correct.
