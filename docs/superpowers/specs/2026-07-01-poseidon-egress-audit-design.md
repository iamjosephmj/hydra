# Poseidon egress audit in the hydra sample — design

Date: 2026-07-01

## Goal

Integrate [Poseidon](https://github.com/iamjosephmj/Poseidon) into the hydra
`sample` app, turn on the `INTERNET` permission for transparency, and surface a
live per-SDK **outbound-egress audit** panel in the main Compose UI.

Poseidon (`tech.ssemaj.poseidon` v0.1.4) is a build-time Gradle plugin — like
hydra itself — that weaves outbound network calls and emits an audit event
stream. It ships data only (no UI); the host app builds its own UI over the
`Observer` sink.

## Decisions (confirmed with user)

- **Mode: `monitor`** — watch and log every outbound request, block nothing.
  Matches the transparency/audit intent.
- **No traffic probe** — the sample keeps making no network calls of its own.
  The egress panel therefore renders an **empty "silent on the wire" state**
  until real traffic occurs. This is intentional and reinforces hydra's
  zero-egress ethos; the plumbing is wired so any future traffic populates it.
- **Full coverage tier** (`injectNative = true`, `nativeDnsCorrelation = true`)
  — JVM + native libc + seccomp weaving, per user request.

## Known risk — Full tier vs hydra integrity

Poseidon's native tier rewrites `.so` files (ELF `DT_NEEDED` injection) and
installs a seccomp `USER_NOTIF` filter at process start. Hydra bakes a native
integrity baseline and re-signs the assembled APK, killing the process on
tamper.

- **Build-order theory (why it *should* compose):** Poseidon weaves during
  compile/merge; hydra bakes the *assembled* APK afterward, so hydra's baseline
  already includes Poseidon's modified `.so`. Integrity should therefore pass.
- **Residual runtime risk:** hydra's hooking / syscall-surface checks could still
  interpret Poseidon's seccomp filter as tampering and kill the process.
- **Mitigation:** `injectNative` is a one-line toggle. If the baked release
  crashes on a clean device, flip it to `false` (JVM-only) and retest.

This risk is only fully resolvable **on a clean physical device** — the build
succeeding is necessary but not sufficient.

## Changes

### 1. Build wiring
- `settings.gradle.kts`: add `maven("https://jitpack.io")` to both
  `pluginManagement.repositories` and `dependencyResolutionManagement.repositories`.
  Add a `resolutionStrategy.eachPlugin` fallback mapping for
  `tech.ssemaj.poseidon` to its JitPack module if the id does not resolve
  directly.
- `sample/build.gradle.kts`: apply `id("tech.ssemaj.poseidon") version "0.1.4"`;
  add `poseidon { injectNative = true; nativeDnsCorrelation = true }`. If the
  runtime classes are not on the compile classpath automatically, add an explicit
  `implementation` on the JitPack runtime module.

### 2. Permission + policy
- `sample/src/main/AndroidManifest.xml`: add
  `<uses-permission android:name="android.permission.INTERNET"/>`.
- New `sample/src/main/res/xml/poseidon_policy.xml`: `mode="monitor"` plus a
  documentary `<allow host>` for the config's `api_base`.
- Reference the policy via
  `<meta-data android:name="tech.ssemaj.poseidon.policy" android:resource="@xml/poseidon_policy"/>`
  inside `<application>`.

### 3. Event capture
- New `HydraSampleApp : Application`, registered via `android:name` in the
  manifest. In `onCreate` it calls
  `Observer.addSink { e -> EgressLog.record(e) }`.
- New `EgressLog` singleton: holds a `MutableStateFlow<List<EgressEntry>>` where
  `EgressEntry(tier: String, host: String, blocked: Boolean)` is mapped from
  `EgressEvent`. Updates are thread-safe (the sink is called on an arbitrary
  thread). Registering in the Application (not the Activity) captures events that
  fire before the UI exists.

### 4. UI panel
- `MainActivity.kt`: new arcade-styled section **"◈ OUTBOUND EGRESS AUDIT ◈"**
  below the encrypted-asset panel, collecting `EgressLog.flow` via
  `collectAsState()` (no new dependency — StateFlow support is in compose-runtime).
- Row format `[TIER] host → ALLOW/BLOCK`; ALLOW green, BLOCK magenta, monospace
  neon to match the existing theme.
- Empty state: panel reading `▓ NO EGRESS OBSERVED ▓` / `SILENT ON THE WIRE`.

## Verification

- **Build:** `./gradlew :sample:assembleRelease` succeeds with both plugins
  applied (catches plugin resolution + native-injection build failures).
- **On-device (critical for Full tier):** install the baked release on a clean
  device; confirm it launches without the RASP killing it and the egress panel
  shows its empty state. If it crashes, set `injectNative = false` and retest.

## Out of scope

- No traffic-generating probe.
- No enforce-mode allow-listing beyond the documentary policy entry.
- No routing of egress events to an external metrics sink (Logcat + in-app panel
  only).
