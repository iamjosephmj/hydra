package io.ssemaj.rasp.sample

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Minimal consumer of DeviceIntelligenceRASP.
 *
 * There is NO API to call — applying the Gradle plugin + depending on the AAR is
 * the entire integration. The plugin injects a randomized bootstrap that loads
 * libdicore.so and starts the native RASP at process creation; on a tampered /
 * rooted / unlocked device the process is terminated before this UI is useful.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }
        root.addView(TextView(this).apply {
            text = "DeviceIntelligenceRASP — consumed as a binary.\n" +
                "Protection runs autonomously in-process; there is no API."
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
        })
        setContentView(root)
    }
}
