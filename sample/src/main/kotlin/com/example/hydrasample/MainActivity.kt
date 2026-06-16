package com.example.hydrasample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.iamjosephmj.hydra.Hydra

// ── arcade neon palette ───────────────────────────────────────────────────────
private val Bg = Color(0xFF0D0221)
private val BgTop = Color(0xFF1A0B2E)
private val Magenta = Color(0xFFFF2E97)
private val Cyan = Color(0xFF00F0FF)
private val Purple = Color(0xFFB026FF)
private val Yellow = Color(0xFFFFD700)
private val Green = Color(0xFF39FF14)
private val Panel = Color(0xFF160A2B)

private fun glow(c: Color) = Shadow(color = c, offset = Offset(0f, 0f), blurRadius = 26f)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ArcadeScreen() }
    }
}

@Composable
private fun ArcadeScreen() {
    // These literals were ENCRYPTED at build time by the hydra plugin. The dex
    // ships only ciphertext; Hydra.secret(...) decrypts here, at runtime, through
    // the obfuscated native core.
    val secrets = remember {
        listOf("apiUrl", "apiKey").map { name ->
            name to runCatching { Hydra.secret(name) }
                .getOrElse { "⚠ unavailable (native not ready)" }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, Bg, Color.Black))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BasicText(
                "HYDRA",
                style = TextStyle(
                    color = Magenta, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold, fontSize = 64.sp,
                    letterSpacing = 8.sp, textAlign = TextAlign.Center,
                    shadow = glow(Cyan),
                ),
            )
            Spacer(Modifier.height(6.dp))
            BasicText(
                "▶ RUNTIME SELF-PROTECTION",
                style = TextStyle(
                    color = Cyan, fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp, letterSpacing = 4.sp, shadow = glow(Cyan),
                ),
            )

            Spacer(Modifier.height(28.dp))
            BasicText(
                "════════  SECRET VAULT  ════════",
                style = TextStyle(
                    color = Purple, fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp, letterSpacing = 1.sp, shadow = glow(Purple),
                ),
            )
            Spacer(Modifier.height(14.dp))

            secrets.forEach { (name, value) ->
                SecretPanel(name, value)
                Spacer(Modifier.height(14.dp))
            }

            Spacer(Modifier.height(20.dp))
            BasicText(
                "● INSERT COIN ●",
                style = TextStyle(
                    color = Yellow, fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp, letterSpacing = 3.sp, shadow = glow(Yellow),
                ),
            )
        }
    }
}

@Composable
private fun SecretPanel(name: String, value: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .border(2.dp, Magenta, RoundedCornerShape(6.dp))
            .background(Panel, RoundedCornerShape(6.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column {
            BasicText(
                "🔓 $name",
                style = TextStyle(
                    color = Yellow, fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                ),
            )
            Spacer(Modifier.height(6.dp))
            BasicText(
                value,
                style = TextStyle(
                    color = Cyan, fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp, shadow = glow(Cyan),
                ),
            )
        }
    }
}
