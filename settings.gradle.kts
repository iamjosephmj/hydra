pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
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
        google()
        mavenCentral()
    }
}

rootProject.name = "hydra"
include(":sample")
