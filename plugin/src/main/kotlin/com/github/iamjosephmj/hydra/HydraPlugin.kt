package com.github.iamjosephmj.hydra

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import io.ssemaj.deviceintelligence.gradle.DeviceIntelligenceExtension
import io.ssemaj.deviceintelligence.gradle.DeviceIntelligencePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/**
 * hydra entry point. Vendors the released DeviceIntelligence 4.0.0 runtime and
 * delegates the baking to the bundled [DeviceIntelligencePlugin].
 *
 * Self-contained delivery: the bundled plugin auto-adds
 * `implementation("io.ssemaj.rasp:deviceintelligence:4.0.0")`. We extract the
 * vendored AAR + a generated POM into a build-local Maven repo under that exact
 * coordinate and register the repo, so the auto-runtime resolves offline with no
 * dependency on the DeviceIntelligenceRASP origin repo.
 */
class HydraPlugin : Plugin<Project> {
    private companion object {
        const val RUNTIME_GROUP = "io.ssemaj.rasp"
        const val RUNTIME_ARTIFACT = "deviceintelligence"
        const val RUNTIME_VERSION = "4.0.0"
        const val AAR_RESOURCE = "/hydra/deviceintelligence-4.0.0.aar"
    }

    override fun apply(project: Project) {
        val hydra = project.extensions.create("hydra", HydraExtension::class.java).apply {
            verbose.convention(false)
            encryptStrings.convention(emptySet())
            encryptAssets.convention(emptySet())
            enableVpnDetection.convention(false)
            enableBiometricsDetection.convention(false)
            appBundle.enabled.convention(false)
        }

        // The underlying DeviceIntelligencePlugin references AGP types at
        // instantiation, so it can only be applied once an Android application
        // or library plugin is present. Wiring inside withId also matches its
        // intent — there is nothing to bake without an Android module.
        project.plugins.withId("com.android.application") { wire(project, hydra) }
        project.plugins.withId("com.android.library") { wire(project, hydra) }
    }

    private fun wire(project: Project, hydra: HydraExtension) {
        injectRuntimeRepo(project)
        project.pluginManager.apply(DeviceIntelligencePlugin::class.java)
        forwardConfig(project, hydra)
        wireSecrets(project, hydra)
    }

    /**
     * Per-variant: generate `Hydra.java` (encrypted named secrets) and add it to
     * the variant's Java sources so the app can call `Hydra.secret("name")`.
     */
    private fun wireSecrets(project: Project, hydra: HydraExtension) {
        val components = project.extensions
            .findByType(ApplicationAndroidComponentsExtension::class.java) ?: return
        components.onVariants { variant ->
            val cap = variant.name.replaceFirstChar { it.uppercase() }
            val task = project.tasks.register<GenerateHydraSecretsTask>(
                "generate${cap}HydraSecrets",
            ) { secrets.set(hydra.secrets) }
            variant.sources.java?.addGeneratedSourceDirectory(
                task,
                GenerateHydraSecretsTask::outputDir,
            )
        }
    }

    /** Materialise the vendored AAR + POM into a build-local m2 and register it. */
    private fun injectRuntimeRepo(project: Project) {
        val repoDir = project.rootProject.layout.buildDirectory
            .dir("hydra/m2").get().asFile
        val artDir = repoDir.resolve(
            "${RUNTIME_GROUP.replace('.', '/')}/$RUNTIME_ARTIFACT/$RUNTIME_VERSION"
        ).apply { mkdirs() }

        val aar = artDir.resolve("$RUNTIME_ARTIFACT-$RUNTIME_VERSION.aar")
        if (!aar.exists()) {
            javaClass.getResourceAsStream(AAR_RESOURCE).use { input ->
                requireNotNull(input) { "hydra: bundled AAR resource $AAR_RESOURCE missing" }
                aar.outputStream().use { input.copyTo(it) }
            }
        }
        // The 4.0.0 runtime module declares no dependencies → POM needs none.
        artDir.resolve("$RUNTIME_ARTIFACT-$RUNTIME_VERSION.pom").writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>
              <groupId>$RUNTIME_GROUP</groupId>
              <artifactId>$RUNTIME_ARTIFACT</artifactId>
              <version>$RUNTIME_VERSION</version>
              <packaging>aar</packaging>
            </project>
            """.trimIndent() + "\n"
        )

        project.repositories.maven {
            name = "hydraRuntime"
            setUrl(repoDir.toURI())
            content { includeGroup(RUNTIME_GROUP) }
        }
        // Declaring any project repository makes Gradle's default PREFER_PROJECT
        // mode ignore the settings repositories for this project — which would
        // break AGP's own resolution (aapt2 from google(), etc.). Re-provide the
        // standard repos at project level so the host needs no settings changes.
        // (A host that pins FAIL_ON_PROJECT_REPOS must add the runtime repo in
        // settings instead — see the README.)
        project.repositories.google()
        project.repositories.mavenCentral()
    }

    /** Forward the hydra DSL onto the underlying deviceintelligence extension. */
    private fun forwardConfig(project: Project, hydra: HydraExtension) {
        val di = project.extensions.getByType(DeviceIntelligenceExtension::class.java)
        di.verbose.set(hydra.verbose)
        di.encryptStrings.set(hydra.encryptStrings)
        di.encryptAssets.set(hydra.encryptAssets)
        di.enableVpnDetection.set(hydra.enableVpnDetection)
        di.enableBiometricsDetection.set(hydra.enableBiometricsDetection)
        di.appBundle.enabled.set(hydra.appBundle.enabled)
        di.appBundle.playSigningCertSha256.set(hydra.appBundle.playSigningCertSha256)
    }
}
