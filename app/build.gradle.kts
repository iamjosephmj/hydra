import io.ssemaj.deviceintelligence.gradle.DeviceIntelligenceExtension

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// The DeviceIntelligenceRASP plugin is on the root buildscript classpath (from
// libs/), so we apply it by id rather than via the version-resolved plugins {}
// block. Applying it auto-generates the randomized bootstrap, bakes the APK
// fingerprint (via the closed baker), and wires the lethal RASP into the build.
apply(plugin = "io.ssemaj.deviceintelligence")

android {
    namespace = "io.ssemaj.rasp.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.ssemaj.rasp.sample"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "3.0.0"
    }

    // The plugin needs a fully-resolved signingConfig per buildType to re-sign the
    // fingerprint-injected APK. A committed debug-style keystore (android /
    // androiddebugkey / android) keeps this repo self-contained; a real app points
    // this at its production keystore.
    signingConfigs {
        create("sample") {
            storeFile = rootProject.file("app/keystore/sample.jks")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("sample")
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("sample")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

// The runtime AAR (libdicore.so + the JVM shims) is the provided binary in libs/.
// The plugin's auto-runtime-dependency is disabled via the gradle.properties flag
// `deviceintelligence.disableAutoRuntimeDependency=true` (read at apply time — the
// DSL flag would be set too late), and we supply the AAR ourselves via flatDir.
configure<DeviceIntelligenceExtension> {
    verbose.set(true)
}

dependencies {
    add("implementation", mapOf("name" to "deviceintelligence-3.0.0", "ext" to "aar"))
}
