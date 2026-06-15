plugins {
    id("com.android.application")
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
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("demo")
        }
    }
}

hydra {
    verbose.set(true)
}
