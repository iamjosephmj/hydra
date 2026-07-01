pluginManagement {
    repositories {
        // Poseidon's runtime AARs live on JitPack; its *Gradle plugin* is a
        // standalone build that upstream publishes to the local Maven cache
        // (`./gradlew publishToMavenLocal` inside Poseidon's `poseidon-gradle-plugin`).
        // See docs/superpowers/specs — the plugin is NOT on JitPack.
        mavenLocal()
        maven("https://jitpack.io")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // The plugin publishes under group `tech.ssemaj.poseidon`, module
    // `poseidon-gradle-plugin` — map the id to that concrete coordinate so
    // `id("tech.ssemaj.poseidon")` resolves from mavenLocal.
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "tech.ssemaj.poseidon") {
                val version = requested.version ?: "0.1.4"
                useModule("tech.ssemaj.poseidon:poseidon-gradle-plugin:$version")
            }
        }
    }
    // Plugin is an included (composite) build so the sample can apply it by id
    // in-repo without publishing, and it stays independently buildable/publishable.
    includeBuild("plugin")
}
dependencyResolutionManagement {
    // Default mode (PREFER_PROJECT): allow the hydra plugin to inject its
    // build-local runtime repo at the project level. A host that pins
    // FAIL_ON_PROJECT_REPOS must instead add the runtime repo in settings
    // (documented in the README).
    repositories {
        // Poseidon runtime AAR (poseidon-all for the Full/native tier, or
        // poseidon-core for JVM-only). Published to JitPack upstream and mirrored
        // in the local Maven cache alongside the plugin.
        mavenLocal()
        maven("https://jitpack.io")
        google()
        mavenCentral()
    }
}

rootProject.name = "hydra"
include(":sample")
