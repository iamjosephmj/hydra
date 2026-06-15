pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // The DeviceIntelligenceRASP runtime AAR (libdicore.so + the logic-free
        // JVM shims) is consumed as a provided binary from `libs/` — no source.
        flatDir { dirs("${rootDir}/libs") }
    }
}

rootProject.name = "DeviceIntelligenceRASP-Sample"
include(":app")
