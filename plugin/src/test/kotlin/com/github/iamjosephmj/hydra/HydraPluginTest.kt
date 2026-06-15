package com.github.iamjosephmj.hydra

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class HydraPluginTest {
    @Test
    fun `applying hydra to a bare project succeeds and creates the hydra extension`(@TempDir dir: File) {
        File(dir, "settings.gradle.kts").writeText("""rootProject.name = "probe"""")
        File(dir, "build.gradle.kts").writeText(
            """
            plugins { id("com.github.iamjosephmj.hydra") }
            // No Android plugin here: assert hydra applies cleanly and creates
            // its DSL extension. The runtime-repo injection + DeviceIntelligence
            // delegation only wire when an Android plugin is present, which the
            // sample app's release build exercises end-to-end.
            tasks.register("probe") {
                doLast {
                    require(project.extensions.findByName("hydra") != null) {
                        "hydra extension was not created"
                    }
                }
            }
            """.trimIndent()
        )
        val result = GradleRunner.create()
            .withProjectDir(dir)
            .withPluginClasspath()
            .withArguments("probe")
            .build()
        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
    }
}
