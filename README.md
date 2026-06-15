# hydra

One Gradle plugin that bakes [DeviceIntelligence RASP](https://github.com/iamjosephmj/DeviceIntelligenceRASP)
into **any** Android app — no source, no runtime code to write. Apply it and the
built APK ships the heavily OLLVM-obfuscated native core (`libdicore.so`), a
per-build integrity baseline, and a randomized bootstrap that starts the
protection at process creation.

## What it checks

All checks run **natively** at startup. A confirmed **CRITICAL** finding
terminates the process (lethal by default — no advisory/observe mode).

- **Root checks**
- **Hooking checks**
- **Cloning checks**
- **Integrity checks**
- **Hardening**

## Integrate

**1.** Add the JitPack repo in `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        maven("https://jitpack.io")
        google(); mavenCentral(); gradlePluginPortal()
    }
}
```

**2.** Apply the plugin in your app module's `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("com.github.iamjosephmj.hydra") version "1.0.0"
}
```

That is the entire integration — no dependencies, no init call. Optional config:

```kotlin
hydra {
    verbose.set(true)                   // log the baking steps
    enableVpnDetection.set(true)        // inject ACCESS_NETWORK_STATE
    enableBiometricsDetection.set(true) // inject USE_BIOMETRIC
}
```

**Requirement:** your `release` buildType must have a fully-resolved
`signingConfig` — hydra re-signs the instrumented APK; without a keystore the
release build fails. (If you pin `dependencyResolutionManagement` to
`FAIL_ON_PROJECT_REPOS`, also add `maven("https://jitpack.io")` to the settings
`repositories` block so the runtime AAR resolves.)

> Plugin id not resolving via JitPack? Map it explicitly in
> `pluginManagement { resolutionStrategy { eachPlugin { if (requested.id.id == "com.github.iamjosephmj.hydra") useModule("com.github.iamjosephmj.hydra:com.github.iamjosephmj.hydra.gradle.plugin:1.0.0") } } }`.

## String encryption (optional)

hydra can encrypt selected `String` constants so they never appear as plaintext
in the dex. Each build uses a fresh random key; the key is **re-derived in the
obfuscated native code** at runtime (it is never shipped in the dex), and the
ciphertext differs every build.

Enable it and allowlist the exact strings to encrypt:

```bash
./gradlew :app:assembleRelease -Pdi.dexstrings=true
```

```kotlin
hydra {
    encryptStrings.add("https://api.your-backend.example/v1")
    encryptStrings.add("YOUR_API_SECRET_HEADER")
}
```

Only **exact-match** allowlisted strings are transformed (efficiency — it does
not touch every string in the app).

**Scope caveat (important for your own project):** as shipped, the transform
only rewrites classes in the `io.ssemaj.*` package (the RASP runtime). It does
**not** yet encrypt string constants in your own app's packages
(`com.yourcompany.*`). To protect your app's own strings, the transform's
class filter must be widened to your package — a small plugin change, not a
config flag. Open an issue / ask if you want hydra to cover your package.

## Heads-up: enforcement is unconditional

On a **tampered / rooted / hooked / cloned** device the process is terminated
(an organic-looking native crash) at startup. On a **genuine** device nothing is
critical and the app runs normally. Expect a baked APK to crash on a rooted test
device — that is the RASP working as intended.

## Repo layout

| Path | Role |
|---|---|
| `plugin/` | the hydra Gradle plugin (composite included build) |
| `plugin/libs/` | vendored 3.0.0 plugin + baker jars |
| `plugin/src/main/resources/hydra/` | vendored 3.0.0 runtime AAR (the `.so` payload) |
| `sample/` | minimal app proving the "any app" claim end-to-end |
| `docs/` | design spec |
