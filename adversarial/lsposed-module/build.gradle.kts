// Standalone LSPosed module (attack 4). Build separately — it is intentionally not
// wired into hydra's root settings.gradle so the main build stays clean.
//   ./gradlew assembleRelease   (from this directory, or add it as an includeBuild)
plugins {
    id("com.android.application") version "8.13.2"
    id("org.jetbrains.kotlin.android") version "2.2.10"
}
android {
    namespace = "com.example.hydra.lsattack"
    compileSdk = 36
    defaultConfig { applicationId = "com.example.hydra.lsattack"; minSdk = 28; targetSdk = 36; versionCode = 1; versionName = "0.1" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
repositories { google(); mavenCentral(); maven("https://api.xposed.info/") }
dependencies {
    // Xposed API — compileOnly (LSPosed provides it at runtime).
    compileOnly("de.robv.android.xposed:api:82")
}
