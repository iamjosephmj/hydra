// red-team LSPosed module — Java-boundary neutralizations of the DI runtime.
// Standalone build (not wired into any root settings):
//   ./gradlew assembleRelease   (from this directory)
plugins {
    id("com.android.application") version "8.13.2"
    id("org.jetbrains.kotlin.android") version "2.2.10"
}
android {
    namespace = "redteam.lsp"
    compileSdk = 36
    defaultConfig { applicationId = "redteam.lsp.dineutralize"; minSdk = 28; targetSdk = 36; versionCode = 1; versionName = "0.1" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
repositories { google(); mavenCentral(); maven("https://api.xposed.info/") }
dependencies { compileOnly("de.robv.android.xposed:api:82") }
