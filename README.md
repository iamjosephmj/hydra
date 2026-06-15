# DeviceIntelligenceRASP — Sample consumer

A minimal, self-contained Android app that **consumes DeviceIntelligenceRASP as
provided binaries** — no source. It demonstrates the entire integration:
*apply the Gradle plugin + depend on the runtime AAR, and write nothing else.*

## What's provided (in `libs/`)
These are the released binaries from the (private) DeviceIntelligenceRASP repo —
this sample does not contain their source:

| Artifact | Role |
|---|---|
| `deviceintelligence-3.0.0.aar` | runtime library — `libdicore.so` + the logic-free JVM shims |
| `deviceintelligence-gradle-3.0.0.jar` | the build-time Gradle plugin |
| `deviceintelligence-baker-3.0.0.jar` | closed key-derivation helper the plugin depends on |

## How it's wired
- `settings.gradle.kts` adds a `flatDir` repo over `libs/` for the AAR.
- The root `build.gradle.kts` puts the plugin + baker (+ its `apksig` build dep)
  on the buildscript classpath.
- `app/build.gradle.kts` applies `io.ssemaj.deviceintelligence`, disables the
  plugin's auto-runtime-dependency, and supplies the AAR from `flatDir`.

That's it — no detection API, no callbacks. The plugin injects a randomized
bootstrap that loads `libdicore.so` and starts the native RASP at process
creation.

## Build
```bash
./gradlew :app:assembleDebug
```
The output APK is RASP-protected.

## Heads-up: enforcement is unconditional
DeviceIntelligenceRASP is **lethal by default** — there is no advisory/observe
mode. On a **tampered / rooted / bootloader-unlocked** device the process is
terminated (organic native crash) at startup. On a **genuine** device nothing is
critical, so the app runs normally. Expect the built APK to crash on a rooted
test device — that is the RASP working as intended.

## Security note
These binaries are closed but JVM/native — i.e. RE raises the cost ("delay"),
it does not make secrets unrecoverable. The `baker` JAR holds the build-time
env-KEK phrases; treat that as a known, accepted property of distributing the
binaries publicly. The kill paths do not depend on the binaries being secret.
