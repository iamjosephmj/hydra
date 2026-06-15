# hydra — redistributable RASP Gradle plugin

**Date:** 2026-06-16
**Status:** Approved (design)
**Origin:** DeviceIntelligenceRASP 3.0.0

## Goal

Ship a standalone GitHub repo, `hydra`, containing a Gradle plugin that any
Android application can apply to "bake" the DeviceIntelligence RASP checks into
its APK at build time — fingerprinting, manifest injection, APK instrumentation,
dex string encryption, the obfuscated `libdicore.so` native runtime, and a
re-sign with the host app's own signing config.

hydra does **not** re-implement or rebuild anything. It vendors the already
released DeviceIntelligenceRASP 3.0.0 binaries and delegates the baking to the
proven `DeviceIntelligencePlugin` logic.

## Decisions (locked)

| Decision | Choice |
|---|---|
| Repo | `github.com/iamjosephmj/hydra` |
| Maven base | `com.github.iamjosephmj:hydra` (JitPack) |
| Plugin id | `com.github.iamjosephmj.hydra` |
| Runtime delivery | Self-contained — vendor + republish the AAR under hydra coords |
| Distribution | JitPack |
| Sample app | Yes — minimal demo Android app |

## Architecture

Thin wrapper plugin over the existing, already-generic `DeviceIntelligencePlugin`.

```
hydra/
├── settings.gradle.kts            # includes :plugin and :sample
├── build.gradle.kts               # JitPack / maven-publish wiring
├── plugin/
│   ├── build.gradle.kts           # java-gradle-plugin; vendors the jars
│   ├── libs/
│   │   ├── deviceintelligence-gradle-3.0.0.jar   # baking logic (reused)
│   │   └── deviceintelligence-baker-3.0.0.jar    # closed key derivation
│   ├── runtime/deviceintelligence-3.0.0.aar      # obfuscated .so payload
│   └── src/main/kotlin/com/github/iamjosephmj/hydra/HydraPlugin.kt
├── sample/                        # minimal Android app (integration proof)
├── docs/                          # this spec
└── README.md
```

### Components

1. **HydraPlugin** (`com.github.iamjosephmj.hydra`, ~80 lines). On `apply(project)`:
   1. Apply the bundled `DeviceIntelligencePlugin` with
      `disableAutoRuntimeDependency = true` (it instruments + re-signs but does
      not resolve its own AAR from the io.ssemaj coordinates).
   2. Add hydra's republished runtime AAR to the host's `implementation`,
      resolved from the same JitPack repo the host already added for the plugin.
   3. Expose a `hydra { }` extension forwarding to the underlying
      `deviceintelligence { }` config (verbose, detectors, etc.).

2. **Vendored binaries** — the three 3.0.0 artifacts, committed to the repo.
   `deviceintelligence-gradle-3.0.0.jar` and `baker` go on the plugin's
   compile + runtime classpath; the AAR is a publishable payload.

3. **Republished runtime artifacts** — hydra's build publishes the vendored AAR
   and baker under `com.github.iamjosephmj.hydra:hydra-runtime` and
   `:hydra-baker`. JitPack serves the plugin marker + runtime + baker under one
   coordinate base. A host references only hydra, never DeviceIntelligenceRASP.

4. **Sample app** — applies the plugin; its release build is the integration
   test (instrumented APK that contains `libdicore.so` + the injected baseline
   asset and is re-signed).

## Data flow (host app)

```
settings.gradle.kts:   pluginManagement + dependencyResolutionManagement
                       add maven("https://jitpack.io")
app/build.gradle.kts:  plugins { id("com.android.application")
                                  id("com.github.iamjosephmj.hydra") version "<tag>" }
configure time:        HydraPlugin → DeviceIntelligencePlugin logic
                       → per-variant fingerprint / instrument / dex-transform tasks
                       → runtime AAR added to implementation
build time:            APK fingerprinted → manifest injected → instrumented with
                       baked baseline → dex strings encrypted → re-signed with the
                       host's signingConfig → hardened APK with libdicore.so
```

## Distribution

Tag `hydra` → JitPack builds it. The build is pure JVM plus vendored binaries —
**no OLLVM / native build runs on JitPack**. Host apps consume via the JitPack
repo. README documents a `buildscript { classpath(...) }` fallback because
plugin-id resolution through JitPack can require it.

## Error handling & constraints

- **Host release buildType must have a fully-resolved `signingConfig`** — the
  baking re-signs the APK (inherited DeviceIntelligencePlugin constraint).
  Documented prominently in the README.
- If the host opts out via `hydra { /* disable */ }`, the plugin degrades to a
  no-op runtime add (forwarded to the underlying extension).

## Testing

- **Integration:** `sample/` release build produces an instrumented APK;
  verify it contains `lib/*/libdicore.so` + the injected baseline asset and
  carries a v1+v2+v3 signature. Run before tagging.
- **Smoke:** apply the published plugin to a throwaway app from JitPack to
  confirm end-to-end resolution.

## Risks / validation points

1. **JitPack building a plugin that vendors binary jars + republishes an AAR.**
   Validate with a throwaway test tag before the real 1.0.0.
2. **Plugin-id resolution through JitPack** — provide the buildscript-classpath
   fallback.
3. **Host repository mode** (`FAIL_ON_PROJECT_REPOS`) — the JitPack repo lives in
   the host's `settings.gradle.kts`, which is the standard, supported location.

## Out of scope (YAGNI)

- Gradle Plugin Portal publication.
- Re-implementing or rebuilding detection logic or native code.
- New runtime features beyond what 3.0.0 already produces.
