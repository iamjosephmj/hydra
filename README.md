<!-- ════════════════════════════════════════════════════════════════════════ -->
<!--                          H Y D R A   ·   ARCADE                          -->
<!-- ════════════════════════════════════════════════════════════════════════ -->

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:FF0080,45:B026FF,100:00F0FF&height=230&section=header&text=HYDRA&fontSize=96&fontColor=ffffff&animation=fadeIn&fontAlignY=38&desc=RUNTIME%20APPLICATION%20SELF-PROTECTION%20//%20ONE%20GRADLE%20PLUGIN&descAlignY=60&descSize=18" width="100%"/>

<a href="https://github.com/iamjosephmj/hydra">
  <img src="https://readme-typing-svg.demolab.com?font=Press+Start+2P&size=17&duration=2800&pause=900&color=00F0FF&center=true&vCenter=true&width=860&height=80&lines=INSERT+PLUGIN+TO+CONTINUE...;ROOT+%2F+HOOK+%2F+CLONE+%E2%86%92+GAME+OVER;ONE+LINE+%E2%80%94+YOUR+APK+SELF-DEFENDS" alt="typing"/>
</a>

<p>
  <img src="https://img.shields.io/badge/LICENSE-APACHE_2.0-FF0080?style=for-the-badge&labelColor=0D0221"/>
  <img src="https://img.shields.io/badge/API-28+-00F0FF?style=for-the-badge&logo=android&logoColor=white&labelColor=0D0221"/>
  <a href="https://jitpack.io/#iamjosephmj/hydra"><img src="https://img.shields.io/jitpack/version/com.github.iamjosephmj/hydra?style=for-the-badge&color=B026FF&labelColor=0D0221&label=JITPACK"/></a>
  <img src="https://img.shields.io/badge/KOTLIN-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white&labelColor=0D0221"/>
  <img src="https://img.shields.io/badge/GRADLE-02303A?style=for-the-badge&logo=gradle&logoColor=00F0FF&labelColor=0D0221"/>
</p>

<p>
  <img src="https://img.shields.io/badge/TRACKING-ZERO-39FF14?style=for-the-badge&labelColor=0D0221"/>
  <img src="https://img.shields.io/badge/100%25-ON--DEVICE-FFD700?style=for-the-badge&labelColor=0D0221"/>
  <img src="https://img.shields.io/badge/GDPR-READY-00F0FF?style=for-the-badge&labelColor=0D0221"/>
  <a href="https://github.com/iamjosephmj/hydra/actions/workflows/release-apk.yml"><img src="https://img.shields.io/github/actions/workflow/status/iamjosephmj/hydra/release-apk.yml?style=for-the-badge&labelColor=0D0221&color=B026FF&label=CI"/></a>
</p>

```
        ╔═══════════════════════════════════════════════════════════════╗
        ║   ▶ P1     ROOT ▰▰▰   HOOK ▰▰▰   CLONE ▰▰▰   INTEGRITY ▰▰▰   ║
        ║                                                    1 CREDIT   ║
        ╚═══════════════════════════════════════════════════════════════╝
```

### `🕹️  A Gradle-plugin RASP for Android  🕹️`

Add it like any other build plugin and it **dynamically injects** a hardened
native protection layer straight into your APK — no security code, no SDK calls,
no servers. **Apply the plugin, build your app, and the output APK comes out
self-defending.**

<br>

🕹️ &nbsp; 👾 &nbsp; 🪙 &nbsp; ⚡ &nbsp; 🛡️ &nbsp; 🔒 &nbsp; 💥 &nbsp; 🎯 &nbsp; 🏆 &nbsp; 👾 &nbsp; 🕹️

</div>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:FF0080,100:00F0FF&height=3" width="100%"/>

## 👾 &nbsp; PLAYER GUIDE

Under the hood, applying hydra bakes a heavily OLLVM-obfuscated native core
(`libdicore.so`), a per-build integrity baseline, and a randomized bootstrap into
your APK. Protection starts at process creation and runs entirely on-device, in
native code.

## 🪙 &nbsp; PHILOSOPHY — *"SOME PROTECTION > NONE"*

Security on a device you don't control is never absolute. A determined,
well-resourced attacker with unlimited time can defeat any client-side
protection — anything that runs can eventually be observed and undone. hydra does
not pretend otherwise.

What it does is **raise the cost.** Most attacks are opportunistic and
tooling-driven; they move on when an app doesn't crack open in five minutes with
the usual tools. An unprotected app is trivially repackaged, hooked, and cloned;
a hydra-protected one forces an attacker through obfuscated native code,
self-verification, and unconditional enforcement first. That is *delay, not
denial* — and for most apps, delay is what changes the economics.

It is also **friction-free.** Protection you can turn on with one plugin line is
protection that actually ships — hydra optimizes for *"good protection, applied"*
over *"perfect protection, skipped."*

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:B026FF,100:FF0080&height=3" width="100%"/>

## 🛡️ &nbsp; SELECT YOUR DEFENSE

<div align="center">

| 🛡️ | 🪝 | 👯 | 👾 | 🔏 | 🧬 |
|:--:|:--:|:--:|:--:|:--:|:--:|
| **ROOT** | **HOOKING** | **CLONE / VIRTUAL** | **EMULATOR** | **INTEGRITY** | **HARDENING** |
| `▰▰▰▰▰` | `▰▰▰▰▰` | `▰▰▰▰▰` | `▰▰▰▰▰` | `▰▰▰▰▰` | `▰▰▰▰▰` |
| 🟢 `ON` | 🟢 `ON` | 🟢 `ON` | 🟢 `ON` | 🟢 `ON` | 🟢 `ON` |

</div>

> All checks run **natively** at startup. A confirmed **CRITICAL** finding terminates the process — **lethal by default**, no advisory/observe mode. ⚠️ **GAME OVER** for tampered devices.

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:00F0FF,100:B026FF&height=3" width="100%"/>

## 🛰️ &nbsp; ZERO TRACKING · GDPR-READY

hydra is **100% on-device**. It collects nothing, transmits nothing, and phones
no one home.

| ✔ | &nbsp; |
|:--:|:--|
| 🚫 | **No network calls.** The runtime declares **no `INTERNET` permission** — it *physically cannot* transmit. No telemetry, no analytics, no crash-reporting SDK, no "phone-home". |
| 📵 | **No identifiers.** No advertising ID, no device fingerprint sent anywhere, no user IDs, no cookies. |
| 🏠 | **Everything stays local.** Every check (root / hooking / cloning / virtualization / emulator / integrity) and the kill decision is computed **on the device** and never leaves it. |
| 🇪🇺 | **GDPR-ready.** hydra processes no personal data off-device and shares nothing with anyone — it adds **zero** data-collection or third-party data-sharing to your app. |

> [!NOTE]
> **Full transparency:** the runtime declares `QUERY_ALL_PACKAGES` so it can
> inspect installed packages for the **cloning check**. That inspection happens
> **only on-device** and the result is **never transmitted** — it feeds the local
> kill decision and nothing else.

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:00F0FF,100:B026FF&height=3" width="100%"/>

## ▶ 🪙 &nbsp; STAGE 1 — INSERT COIN (Download)

[![JitPack](https://img.shields.io/jitpack/version/com.github.iamjosephmj/hydra?style=for-the-badge&color=FFD700&labelColor=0D0221&label=GET%20IT%20ON%20JITPACK)](https://jitpack.io/#iamjosephmj/hydra)

Add the repository in your **`settings.gradle.kts`**:

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

## ▶ 🕹️ &nbsp; STAGE 2 — START GAME (Integrate)

Apply the plugin in your **app module's `build.gradle.kts`**:

```kotlin
plugins {
    id("com.android.application")
    id("com.github.iamjosephmj.hydra") version "1.2.0"
}
```

That's the **entire** integration. No dependency line, no `Hydra.init()`, no code
in your `Application` class. Your next `assembleRelease` produces a self-protecting
APK.

> [!IMPORTANT]
> 🔑 Your `release` build type must have a fully-resolved **`signingConfig`** —
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

<details>
<summary><b>⚙️ &nbsp;Optional config &amp; troubleshooting</b></summary>

<br>

```kotlin
hydra {
    verbose.set(true) // log the baking steps during the build
}
```

**Plugin id not resolving via JitPack?** Map it explicitly in `settings.gradle.kts`:

```kotlin
pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.github.iamjosephmj.hydra") {
                useModule("com.github.iamjosephmj.hydra:com.github.iamjosephmj.hydra.gradle.plugin:1.2.0")
            }
        }
    }
}
```

If you pin `dependencyResolutionManagement` to `FAIL_ON_PROJECT_REPOS`, also add
`maven("https://jitpack.io")` to its `repositories {}` block so the runtime AAR
resolves.

</details>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:FF0080,100:FFD700&height=3" width="100%"/>

## ▶ 💎 &nbsp; BONUS STAGE — 🔐 SECRET VAULT

Keep sensitive strings (API URLs, header names, keys) out of your APK as
plaintext, and read them back in Kotlin. Each value is encrypted at build time
with a **fresh per-build key** that is **re-derived in the obfuscated native
runtime** at decrypt time — the key and the plaintext never touch `classes.dex`,
only ciphertext.

**1️⃣ Stash the secrets** 💾 in your app module's `build.gradle.kts`:

```kotlin
hydra {
    secrets {
        put("apiUrl", "https://api.your-backend.example/v1")
        put("apiKey", "sk_live_abc123")
    }
}
```

**2️⃣ Unlock them in Kotlin** 🔓 via the generated `Hydra` accessor — **off the
main thread** (it blocks until the device clears the first sweep):

```kotlin
import com.github.iamjosephmj.hydra.Hydra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val url = withContext(Dispatchers.IO) { Hydra.secret("apiUrl") }
val key = withContext(Dispatchers.IO) { Hydra.secret("apiKey") }
httpClient.get(url) { header("X-Api-Key", key) }
```

`Hydra.secret(name)` returns the decrypted value at the point of use. In the
built APK, `classes.dex` holds **only ciphertext + the `Hydra.secret(...)` call**
— never the plaintext.

> [!IMPORTANT]
> **Sweep-gated (since `1.2.1`).** `Hydra.secret()` **blocks until the first
> detection sweep completes clean** (zero CRITICAL), then decrypts — the key is
> cryptographically bound to a secret the native runtime publishes *only* on a
> clean device. On a **rooted / hooked / emulated / cloned / tampered** device the
> process is killed before the sweep clears, so the plaintext **never
> materialises — not even for a frame**. Because it blocks, **call it off the main
> thread** (`Dispatchers.IO`); calling it on the UI thread can ANR.

> [!NOTE]
> This is *"no static plaintext"*, not a vault. On a clean device the decrypted
> value lives in memory at runtime, so a runtime hook *there* could read it —
> which is exactly what hydra's hooking/ART checks detect and kill. It removes the
> trivial `strings classes.dex` / jadx extraction and the "screenshot the secret
> on an emulator" path, and raises the bar; for high-value secrets, keep them
> server-side.

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:FFD700,100:FF0080&height=3" width="100%"/>

## 💀 &nbsp; GAME OVER SCREEN (on-device behavior)

On a **tampered / rooted / hooked / cloned / emulated / virtualized** device the
process is terminated (an organic-looking native crash) at startup. Running on an
**emulator** (QEMU / KVM / HAXM / cloud-phone) is blocked too — caught by
CPU-identity probes (`CNTFRQ_EL0` on ARM, the CPUID hypervisor-vendor leaf on x86)
and by software-only key attestation (no hardware-backed TEE evidence). Running
inside a **virtualization / app-cloning / virtual-space** runtime — VirtualApp,
Parallel Space, Dual Space, Island, VirtualXposed, work-profile clones, and the
like — is blocked too: the cloner check flags `apk_path_mismatch` (the APK isn't
where the real install lives) and `data_dir_mount_invalid` (the data dir is the
container's overlay, not the real per-app mount), and the container's loader
hooks trip the native GOT-integrity check. On a **genuine** device nothing is
critical and the app runs normally. Expect a baked APK to crash on a rooted,
emulated, or virtualized test device — that is the RASP working as intended.

## 🎮 &nbsp; TRY THE DEMO

A minimal, runnable host app lives in [`sample/`](sample).

- It declares two `secrets {}` at build time.
- It displays them **decrypted at runtime** via `Hydra.secret(...)`.
- The plaintext is **never** in `classes.dex` — yet it shows up on screen.
- Real device, real round-trip:

<div align="center">
  <img src="art/sample-demo.png" alt="hydra sample — decrypted secrets on screen" width="300"/>
</div>

```bash
./gradlew :sample:assembleRelease
# → sample/build/outputs/apk/release/sample-release.apk   (RASP-protected)
```

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:00F0FF,100:FF0080&height=3" width="100%"/>

## 🏆 &nbsp; HIGH SCORES

<div align="center">

[![Star History Chart](https://api.star-history.com/svg?repos=iamjosephmj/hydra&type=Date&theme=dark)](https://star-history.com/#iamjosephmj/hydra&Date)

### Find this library useful? :heart:

Join the **[stargazers](https://github.com/iamjosephmj/hydra/stargazers)** :star: &nbsp;·&nbsp; **[follow me](https://github.com/iamjosephmj)** for the next creation 🤩

</div>

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:00F0FF,50:B026FF,100:FF0080&height=120&section=footer&text=GAME%20OVER%20%E2%80%94%20CONTINUE%3F&fontSize=22&fontColor=ffffff&fontAlignY=70" width="100%"/>

## 📜 &nbsp; LICENSE

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
