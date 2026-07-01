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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    // ships only ciphertext; Hydra.secret(...) decrypts at runtime through the
    // obfuscated native core — but ONLY after the first detection sweep completes
    // clean. Hydra.secret() therefore BLOCKS until that sweep, so we decrypt off
    // the main thread (Dispatchers.IO) and show a placeholder until it unlocks.
    // On a compromised device (root / hook / emulator / clone / tamper) the
    // process is killed before this ever returns — the plaintext never renders.
    val secrets by produceState(
        initialValue = listOf("apiUrl", "apiKey").map { it to "🔒 …" },
    ) {
        value = withContext(Dispatchers.IO) {
            listOf("apiUrl", "apiKey").map { name ->
                name to runCatching { Hydra.secret(name) }
                    .getOrElse { "⚠ unavailable" }
            }
        }
    }

    // config.json shipped ENCRYPTED — the plaintext is stripped from the APK at
    // build time. Hydra.asset(...) decrypts it through the gated whitebox key,
    // blocking until the first clean sweep (so it runs off the main thread too).
    // On a compromised device the process is killed before this returns.
    val ctx = LocalContext.current
    val assetText by produceState(initialValue = "🔒 …", ctx) {
        value = withContext(Dispatchers.IO) {
            runCatching { String(Hydra.asset(ctx, "config.json")) }
                .getOrElse { "⚠ unavailable" }
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
                "═════  ENCRYPTED ASSET  ═════",
                style = TextStyle(
                    color = Purple, fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp, letterSpacing = 1.sp, shadow = glow(Purple),
                ),
            )
            Spacer(Modifier.height(14.dp))
            SecretPanel("config.json", assetText)

            Spacer(Modifier.height(28.dp))
            BasicText(
                "◈  OUTBOUND EGRESS AUDIT  ◈",
                style = TextStyle(
                    color = Purple, fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp, letterSpacing = 1.sp, shadow = glow(Purple),
                ),
            )
            Spacer(Modifier.height(6.dp))
            BasicText(
                "poseidon · monitor mode",
                style = TextStyle(
                    color = Cyan, fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp, letterSpacing = 2.sp,
                ),
            )
            Spacer(Modifier.height(14.dp))

            // Poseidon logs every outbound connection here (host / tier / decision).
            // The sample makes no network calls of its own, so this stays empty —
            // a live, on-device proof that the app is silent on the wire.
            val egress by EgressLog.flow.collectAsState()
            EgressPanel(egress)

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

@Composable
private fun EgressPanel(entries: List<EgressEntry>) {
    Box(
        Modifier
            .fillMaxWidth()
            .border(2.dp, Cyan, RoundedCornerShape(6.dp))
            .background(Panel, RoundedCornerShape(6.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (entries.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BasicText(
                    "▓ NO EGRESS OBSERVED ▓",
                    style = TextStyle(
                        color = Green, fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp, shadow = glow(Green),
                    ),
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    "SILENT ON THE WIRE",
                    style = TextStyle(
                        color = Cyan, fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp, letterSpacing = 3.sp,
                    ),
                )
            }
        } else {
            Column {
                entries.forEach { e ->
                    BasicText(
                        "[${e.tier}] ${e.host} → ${if (e.blocked) "BLOCK" else "ALLOW"}",
                        style = TextStyle(
                            color = if (e.blocked) Magenta else Green,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            shadow = glow(if (e.blocked) Magenta else Green),
                        ),
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}
