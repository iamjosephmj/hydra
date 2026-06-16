<h1 align="center">hydra</h1>

<p align="center">
  <a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
  <a href="https://android-arsenal.com/api?level=28"><img alt="API" src="https://img.shields.io/badge/API-28%2B-brightgreen.svg?style=flat"/></a>
  <a href="https://jitpack.io/#iamjosephmj/hydra"><img alt="JitPack" src="https://jitpack.io/v/iamjosephmj/hydra.svg"/></a>
  <a href="https://github.com/iamjosephmj"><img alt="Profile" src="https://img.shields.io/badge/GitHub-iamjosephmj-181717?logo=github"/></a>
</p>

<p align="center">
A Gradle-plugin <b>RASP</b> (Runtime Application Self-Protection) for Android. Add it like any other build plugin and it <b>dynamically injects</b> a hardened native protection layer straight into your APK — no security code to write, no SDK to call, no servers. Apply the plugin, build your app, and the output APK comes out self-defending.
</p>

---

Under the hood, applying hydra bakes a heavily OLLVM-obfuscated native core
(`libdicore.so`), a per-build integrity baseline, and a randomized bootstrap into
your APK. The protection starts at process creation and runs entirely on-device,
in native code.

## Philosophy

Security on a device you don't control is never absolute. A determined,
well-resourced attacker with unlimited time can defeat any client-side
protection — anything that runs can eventually be observed and undone. hydra does
not pretend otherwise.

What it does is **raise the cost.** Most attacks are opportunistic and tooling-
driven; they move on when an app doesn't crack open in five minutes with the
usual tools. hydra is built on a simple premise: **some protection is far better
than none.** An unprotected app is trivially repackaged, hooked, and cloned; a
hydra-protected one forces an attacker through obfuscated native code,
self-verification, and unconditional enforcement first. That is *delay, not
denial* — and for most apps, delay is what changes the economics.

It is also **friction-free.** Protection you can turn on with one plugin line is
protection that actually ships — so hydra optimizes for *"good protection,
applied"* over *"perfect protection, skipped."*

## What it checks

- **Root checks**
- **Hooking checks**
- **Cloning checks**
- **Integrity checks**
- **Hardening**

All run **natively** at startup. A confirmed **CRITICAL** finding terminates the
process — lethal by default, no advisory/observe mode.

## Download

[![JitPack](https://jitpack.io/v/iamjosephmj/hydra.svg)](https://jitpack.io/#iamjosephmj/hydra)

hydra is distributed via [JitPack](https://jitpack.io). Add the repository in your
**`settings.gradle.kts`**:

```kotlin
pluginManagement {
    repositories {
        maven("https://jitpack.io")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

## How to integrate

Apply the plugin in your **app module's `build.gradle.kts`** — alongside the
Android application plugin:

```kotlin
plugins {
    id("com.android.application")
    id("com.github.iamjosephmj.hydra") version "1.0.0"
}
```

That is the **entire** integration. No dependency line, no `Hydra.init()`, no
code in your `Application` class. Your next `assembleRelease` produces a
self-protecting APK.

> [!IMPORTANT]
> Your `release` build type must have a fully-resolved **`signingConfig`** —
> hydra re-signs the instrumented APK, so a build without a keystore will fail.

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("your-release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release { signingConfig = signingConfigs.getByName("release") }
    }
}
```

### Configuration (optional)

```kotlin
hydra {
    verbose.set(true)                   // log the baking steps during the build
    enableVpnDetection.set(true)        // inject ACCESS_NETWORK_STATE
    enableBiometricsDetection.set(true) // inject USE_BIOMETRIC
}
```

<details>
<summary>Plugin id not resolving via JitPack?</summary>

JitPack does not always serve the Gradle plugin marker. Map the id explicitly in
**`settings.gradle.kts`**:

```kotlin
pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.github.iamjosephmj.hydra") {
                useModule("com.github.iamjosephmj.hydra:com.github.iamjosephmj.hydra.gradle.plugin:1.0.0")
            }
        }
    }
}
```

If you pin `dependencyResolutionManagement` to `FAIL_ON_PROJECT_REPOS`, also add
`maven("https://jitpack.io")` to its `repositories {}` block so the runtime AAR
resolves.
</details>

## String encryption

hydra can encrypt selected `String` constants so they never appear as plaintext
in the dex. Each build uses a fresh random key that is **re-derived in the
obfuscated native code at runtime** — the key is never shipped in the dex, and
the ciphertext differs on every build.

Enable the transform and allowlist the exact strings to protect:

```bash
./gradlew :app:assembleRelease -Pdi.dexstrings=true
```

```kotlin
hydra {
    encryptStrings.add("https://api.your-backend.example/v1")
    encryptStrings.add("YOUR_API_SECRET_HEADER")
}
```

Only **exact-match** allowlisted strings are transformed.

> [!NOTE]
> As shipped, the transform rewrites classes in the `io.ssemaj.*` package (the
> RASP runtime). To also encrypt constants in **your own app's** packages
> (`com.yourcompany.*`), the transform's class filter must be widened to your
> package — a small plugin change, not a config flag. Open an issue if you want
> hydra to cover your package.

## How it behaves on-device

On a **tampered / rooted / hooked / cloned** device the process is terminated (an
organic-looking native crash) at startup. On a **genuine** device nothing is
critical and the app runs normally. Expect a baked APK to crash on a rooted test
device — that is the RASP working as intended.

## Sample

A minimal, runnable host app lives in [`sample/`](sample) and proves the
"any app" claim end to end:

```bash
./gradlew :sample:assembleRelease
# → sample/build/outputs/apk/release/sample-release.apk  (RASP-protected)
```

## Find this library useful? :heart:

Support it by joining **[stargazers](https://github.com/iamjosephmj/hydra/stargazers)** for this repository. :star: <br>
Also, **[follow me](https://github.com/iamjosephmj)** on GitHub for my next creations! 🤩

# License

```xml
Copyright 2026 Joseph James

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
