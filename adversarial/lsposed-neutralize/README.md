# lsposed-neutralize (red-team — research only)

Java-boundary neutralizations via LSPosed. Two precise modes; scope the module to
`com.example.hydrasample` in LSPosed Manager.

| Mode (`setprop hydra.rt.lsp`) | Hook | Certifies |
|---|---|---|
| `starve-fp` (default) | `FrameworkShim.q(op,arg)` → `ByteArray(0)` for op 6 | **A1**: native fingerprint recovery keeps G2 armed; a real `.so` tamper still kills |
| `noop-orch` | `io.ssemaj.dx.K.o()` → `0` (skip the whole sweep) | funnel-at-entry: is hooking the orchestrator ENTRY caught? (the ART check that would notice runs inside the skipped sweep — circular) |

Build (standalone): `./gradlew assembleRelease` from this dir → an Xposed module APK;
install + enable in LSPosed, scope to the sample, reboot. Read `XposedBridge.log`
(`adb logcat | grep di-rt-lsp`) and the runtime trail together.

> The *native* `collect`→0 funnel hook is a Zygisk attack (`../zygisk-ret-patch`,
> target `collect`), not an LSPosed one — LSPosed cannot hook native C++.
