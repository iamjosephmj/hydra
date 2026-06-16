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
    id("com.github.iamjosephmj.hydra") version "1.0.1"
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
                useModule("com.github.iamjosephmj.hydra:com.github.iamjosephmj.hydra.gradle.plugin:1.0.1")
            }
        }
    }
}
```

If you pin `dependencyResolutionManagement` to `FAIL_ON_PROJECT_REPOS`, also add
`maven("https://jitpack.io")` to its `repositories {}` block so the runtime AAR
resolves.
</details>

## Secrets — encrypted strings with a Kotlin accessor

hydra lets you keep sensitive strings (API URLs, header names, keys) out of your
APK as plaintext. You register them by name in the build; at build time each
value is encrypted under a **fresh per-build key** that is derived in the closed
baker and **re-derived in the obfuscated native runtime** at decrypt time — the
key and the plaintext are never written into `classes.dex`, only ciphertext.

**1. Declare the secrets** in your app module's `build.gradle.kts`:

```kotlin
hydra {
    secrets {
        put("apiUrl", "https://api.your-backend.example/v1")
        put("apiKey", "sk_live_abc123")
    }
}
```

**2. Read them in Kotlin** via the generated `Hydra` accessor:

```kotlin
import com.github.iamjosephmj.hydra.Hydra

val url = Hydra.secret("apiUrl")
val key = Hydra.secret("apiKey")
httpClient.get(url) { header("X-Api-Key", key) }
```

`Hydra.secret(name)` returns the decrypted value at the point of use. In the
built APK, `classes.dex` contains only the ciphertext and the `Hydra.secret(...)`
call — never the plaintext. (Decryption happens through the native runtime, so a
genuine device decrypts transparently while static analysis of the APK yields
nothing readable.)

> [!NOTE]
> This is "no static plaintext", not a secret vault. The decrypted value exists
> in memory at runtime, so a runtime hook could read it — which is exactly what
> hydra's hooking/ART checks are there to detect and kill. It removes the trivial
> `strings classes.dex` / jadx extraction and raises the bar; for high-value
> secrets, keep them server-side.

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
