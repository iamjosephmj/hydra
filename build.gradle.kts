// The DeviceIntelligenceRASP Gradle plugin + its closed `baker` helper are
// consumed as provided JARs from `libs/` (no source). Putting them on the root
// buildscript classpath — together with the plugin's one transitive build dep
// (apksig) — lets the :app module apply the plugin by id.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(files("libs/deviceintelligence-gradle-3.0.0.jar"))
        classpath(files("libs/deviceintelligence-baker-3.0.0.jar"))
        classpath("com.android.tools.build:apksig:8.13.2")
    }
}

plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}
