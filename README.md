# hydra

Bake [DeviceIntelligence RASP](https://github.com/iamjosephmj/DeviceIntelligenceRASP)
checks into **any** Android app with one Gradle plugin id.

hydra vendors the released DeviceIntelligence **3.0.0** runtime (the heavily
OLLVM-obfuscated `libdicore.so`) and instrumentation. Applying the plugin
fingerprints the build, injects the manifest, encrypts dex strings, ships the
native runtime, and **re-signs the APK with your own signing config** — no
source, no extra dependency lines.

## What you get

Apply the plugin to an app and its built APK gains, automatically:
- `lib/{arm64-v8a,armeabi-v7a,x86_64}/libdicore.so` — the obfuscated native RASP core
- a per-build baked baseline asset (`assets/io.ssemaj.deviceintelligence/fingerprint.bin`)
- a randomized native bootstrap that starts the RASP at process creation
- a fresh v1+v2+v3 signature from your signing config

## Usage

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        maven("https://jitpack.io")
        google(); mavenCentral(); gradlePluginPortal()
    }
}
```

`app/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application")
    id("com.github.iamjosephmj.hydra") version "1.0.0"
}
```

That's the whole integration. Optionally configure:
```kotlin
hydra {
    verbose.set(true)                 // log the baking steps
    enableVpnDetection.set(true)      // inject ACCESS_NETWORK_STATE
    enableBiometricsDetection.set(true) // inject USE_BIOMETRIC
    encryptStrings.add("my.sensitive.Constant") // needs -Pdi.dexstrings=true
}
```

### Fallback (if plugin-id resolution via JitPack fails)
JitPack does not always serve the Gradle plugin marker. If `plugins { id(...) }`
cannot resolve, apply via the buildscript classpath instead:

`settings.gradle.kts`:
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

## Requirements

- **Your `release` buildType must have a fully-resolved `signingConfig`.** hydra
  re-signs the instrumented APK; without a keystore the release build fails.
- **Repository mode:** hydra injects a build-local Maven repo for the runtime at
  the project level, so your `dependencyResolutionManagement` must use the
  default `PREFER_PROJECT` mode. If you pin `FAIL_ON_PROJECT_REPOS`, add the
  runtime repo yourself in settings instead:
  ```kotlin
  // settings.gradle.kts → dependencyResolutionManagement.repositories
  maven("https://jitpack.io") // serves io.ssemaj.rasp:deviceintelligence:3.0.0 via hydra
  ```

## Heads-up: enforcement is unconditional

DeviceIntelligence RASP is **lethal by default** — no advisory/observe mode. On a
**tampered / rooted / bootloader-unlocked** device the process is terminated (an
organic-looking native crash) at startup. On a **genuine** device nothing is
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

## Build the sample locally

```bash
export JAVA_HOME=<a JDK 17–23>   # Gradle 8.13; JDK 26 is too new
./gradlew :sample:assembleRelease
```
The output `sample/build/outputs/apk/release/sample-release.apk` is RASP-protected.
