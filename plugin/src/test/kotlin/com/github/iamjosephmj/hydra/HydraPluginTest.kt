package com.github.iamjosephmj.hydra

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class HydraPluginTest {

    @Test
    fun `generated Hydra accessor uses ONLY the sweep-gated decryptor`(@TempDir dir: File) {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register("genSecrets", GenerateHydraSecretsTask::class.java).get()
        task.secrets.put("apiKey", "sk_live_sample_abc123")
        task.outputDir.set(File(dir, "out"))
        task.generate()

        val gen = File(dir, "out/com/github/iamjosephmj/hydra/Hydra.java").readText()
        // Sweep-gated by construction: the only decryptor the generator emits is
        // StrDec.g (blocks until a clean sweep). The ungated StrDec.d must never
        // appear, or a consumer secret could leak before the kill (the 1.2.1 fix).
        assertTrue(gen.contains("StrDec.g("), "generated secret accessor must use the gated StrDec.g")
        assertFalse(gen.contains("StrDec.d("), "generated secret accessor must NOT use the ungated StrDec.d")
    }
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
