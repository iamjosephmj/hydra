plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.github.iamjosephmj.hydra")
}

android {
    namespace = "com.example.hydrasample"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.hydrasample"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    // The baking re-signs the APK, so release needs a fully-resolved signingConfig.
    signingConfigs {
        create("demo") {
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildTypes {
        release {
            // R8: shrink + obfuscate the consumer's own code (a realistic release).
            // The DeviceIntelligence runtime ships consumer ProGuard rules that keep
            // its JNI anchor / up-call shims; proguard-rules.pro keeps the generated
            // hydra secrets accessor. Verified the RASP still kills + the secret stays
            // sweep-gated after minification.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("demo")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}

hydra {
    verbose.set(true)
    secrets {
        put("apiUrl", "https://api.your-backend.example/v1")
        put("apiKey", "sk_live_sample_abc123")
    }
    encryptAssets {
        include("config.json")
    }
    // App Bundle ("bundle mode") integrity — the sample ships as an `.aab`.
    appBundle {
        enabled = true
        // In production, paste the Play App Signing cert SHA-256 (Play Console →
        // App integrity). For this local sample we pin a placeholder; the demo
        // keystore's own signer is auto-included so local installs still pass.
        playSigningCertSha256("AB:CD:EF")
    }
}
