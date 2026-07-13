package com.ether4o4.morsvitaest.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import morsvitaest.composeapp.generated.resources.Res
import morsvitaest.composeapp.generated.resources.archivo_black
import morsvitaest.composeapp.generated.resources.oswald
import morsvitaest.composeapp.generated.resources.share_tech_mono
import org.jetbrains.compose.resources.Font
import kotlin.math.roundToInt
import kotlin.random.Random

// NeverSoft Services boot splash — retro CRT terminal. Fixed brand palette
// (red + black, white + gold accents) — deliberately NOT theme-tracking, so the
// boot screen is identical across every NeverSoft app.
private val NsBlack = Color(0xFF050506)
private val NsRed = Color(0xFFFF1F2E)
private val NsRedDeep = Color(0xFFB3121C)
private val NsWhite = Color(0xFFF6F2EC)
private val NsGold = Color(0xFFE8C268)
private val NsMuted = Color(0xFF7A5A5C)
private val NsRedSoft = Color(0x10FF1F2E)

// Bundled brand fonts (Google Fonts, OFL): terminal mono, display, condensed.
// Loaded via Compose Multiplatform resources (from commonMain composeResources),
// which requires composition context — see rememberSplashFonts below.
private class SplashFonts(
    val mono: FontFamily,
    val display: FontFamily,
    val cond: FontFamily,
)

@Composable
private fun rememberSplashFonts(): SplashFonts {
    val mono = FontFamily(Font(Res.font.share_tech_mono))
    val display = FontFamily(Font(Res.font.archivo_black))
    val cond = FontFamily(Font(Res.font.oswald))
    return remember(mono, display, cond) { SplashFonts(mono, display, cond) }
}

private data class BootStage(val at: Int, val label: String, val sub: String)

// Mirrors the live boot sequence — loads NeverSoft apps by name.
private val BootStages = listOf(
    BootStage(0, "MOUNTING CORE MODULES", "(NeverSoft Core Services)"),
    BootStage(14, "LOADING CROSS TRACE", "(data cross-reference engine)"),
    BootStage(30, "LOADING GO FIND ME", "(OSINT toolkit online)"),
    BootStage(46, "LOADING SPOTLIGHT", "(all-device search index)"),
    BootStage(62, "LOADING COLOUR CEAUXDID", "(LLM agent container)"),
    BootStage(78, "SYNCING APP STORE", "(NeverSoft App Store)"),
    BootStage(90, "VERIFYING SESSION", "(secure // authorized)"),
    BootStage(100, "SERVICES READY", "(welcome to NeverSoft)"),
)

/**
 * A retro CRT-terminal "NeverSoft Services" boot splash: scanlines, red flicker,
 * a chromatic-aberration wordmark, gold corner brackets, and a real 0→100% boot
 * that names the apps as they mount. Runs ~2.5s, then fades and calls [onDone].
 *
 * Strictly a boot splash: shown once over everything at launch, never blocks the
 * app — it always fades itself out and calls [onDone] so the host can drop it.
 */
@Composable
fun NeverSoftSplash(onDone: () -> Unit) {
    val fonts = rememberSplashFonts()
    var progress by remember { mutableFloatStateOf(0f) }
    var visible by remember { mutableStateOf(true) }
    val fade by animateFloatAsState(if (visible) 1f else 0f, tween(340), label = "splashFade")
    val stage = remember(progress.roundToInt()) { BootStages.last { progress >= it.at } }

    LaunchedEffect(Unit) {
        delay(200)
        while (progress < 100f) {
            var step = Random.nextFloat() * 2.6f + 0.9f
            if (Random.nextFloat() < 0.08f) step = 0.15f // believable stall
            progress = (progress + step).coerceAtMost(100f)
            delay(34)
        }
        delay(300) // hold on "SERVICES READY"
        visible = false
        delay(360)
        onDone()
    }

    val flick = rememberInfiniteTransition(label = "crt")
    val flicker by flick.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "flicker",
    )
    val blink by flick.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "blink",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(fade)
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF14090A), Color(0xFF070405), Color(0xFF030203)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth * 0.94f, maxHeight * 0.94f, 640.dp)
            TerminalPanel(
                modifier = Modifier.size(side),
                pct = progress.roundToInt(),
                stage = stage,
                blink = blink,
                fonts = fonts,
            )
        }

        // Scanlines + vignette over everything.
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    var y = 0f
                    val gap = 4.dp.toPx()
                    val lh = 1.dp.toPx()
                    val line = Color(0x47000000)
                    while (y < size.height) {
                        drawRect(line, topLeft = Offset(0f, y), size = Size(size.width, lh))
                        y += gap
                    }
                }
                .background(
                    Brush.radialGradient(
                        0.62f to Color.Transparent,
                        1f to Color(0xBF000000),
                    ),
                ),
        )
        // Faint red CRT flicker.
        Box(
            Modifier
                .fillMaxSize()
                .alpha(flicker * 0.06f)
                .background(NsRed),
        )
    }
}

@Composable
private fun TerminalPanel(
    modifier: Modifier,
    pct: Int,
    stage: BootStage,
    blink: Float,
    fonts: SplashFonts,
) {
    Box(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(0xFF0B0708), Color(0xFF060404))))
                .border(2.dp, NsRed)
                .padding(18.dp),
        ) {
            // Header: glyph box + module select lines.
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier.border(2.dp, NsRed).padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("☎", color = NsRed, fontFamily = fonts.mono, fontSize = 18.sp)
                    Text("通信", color = NsGold, fontFamily = fonts.mono, fontSize = 9.sp, letterSpacing = 1.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("SELECT MODULE:", color = NsGold, fontFamily = fonts.mono, fontSize = 11.sp, letterSpacing = 0.5.sp)
                    Text("[CORE], SYNC, VAULT", color = NsRed, fontFamily = fonts.mono, fontSize = 11.sp, letterSpacing = 0.5.sp)
                    Text("BUILD:11.0   REGION:US", color = NsRed, fontFamily = fonts.mono, fontSize = 11.sp, letterSpacing = 0.5.sp)
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "INITIALIZING SERVICES",
                color = NsWhite,
                fontFamily = fonts.cond,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(6.dp))
            DashedRule()

            Spacer(Modifier.weight(1f))
            // Wordmark with chromatic-aberration copies.
            Wordmark(fonts)
            Spacer(Modifier.weight(1f))

            // Mid status.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stage.label, color = NsWhite, fontFamily = fonts.mono, fontSize = 13.sp, letterSpacing = 0.5.sp)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .alpha(blink)
                        .width(44.dp)
                        .height(12.dp)
                        .background(NsRed),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(stage.sub, color = NsMuted, fontFamily = fonts.mono, fontSize = 11.sp, letterSpacing = 0.5.sp)

            Spacer(Modifier.height(14.dp))
            // Footer: est · loading + bar · NS 11 logo.
            Row(verticalAlignment = Alignment.Bottom) {
                Text("est.2011", color = NsGold, fontFamily = fonts.mono, fontSize = 11.sp, letterSpacing = 0.5.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (pct >= 100) "READY" else "LOADING...",
                            color = NsWhite,
                            fontFamily = fonts.mono,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("$pct%", color = NsGold, fontFamily = fonts.mono, fontSize = 12.sp, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    ProgressBar(pct)
                }
                Spacer(Modifier.width(12.dp))
                Row(
                    modifier = Modifier.border(2.dp, NsGold).padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("☎", color = NsRed, fontFamily = fonts.mono, fontSize = 15.sp)
                    Spacer(Modifier.width(5.dp))
                    Column {
                        Text("NS", color = NsGold, fontFamily = fonts.mono, fontSize = 9.sp, letterSpacing = 1.sp)
                        Text("11", color = NsWhite, fontFamily = fonts.mono, fontSize = 9.sp)
                    }
                }
            }
        }

        // Gold corner brackets.
        BracketCorner(Modifier.align(Alignment.TopStart), top = true, left = true)
        BracketCorner(Modifier.align(Alignment.TopEnd), top = true, left = false)
        BracketCorner(Modifier.align(Alignment.BottomStart), top = false, left = true)
        BracketCorner(Modifier.align(Alignment.BottomEnd), top = false, left = false)
    }
}

@Composable
private fun Wordmark(fonts: SplashFonts) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val base = Modifier
        Text(
            "NeverSoft",
            modifier = base.offset(x = (-3).dp, y = 1.dp),
            color = NsRed,
            fontFamily = fonts.display,
            fontWeight = FontWeight.Black,
            fontSize = 46.sp,
            maxLines = 1,
        )
        Text(
            "NeverSoft",
            modifier = base.offset(x = 3.dp, y = (-1).dp),
            color = NsGold,
            fontFamily = fonts.display,
            fontWeight = FontWeight.Black,
            fontSize = 46.sp,
            maxLines = 1,
        )
        Text(
            "NeverSoft",
            color = NsWhite,
            fontFamily = fonts.display,
            fontWeight = FontWeight.Black,
            fontSize = 46.sp,
            maxLines = 1,
        )
    }
    Spacer(Modifier.height(6.dp))
    Text(
        "SERVICES",
        color = NsGold,
        fontFamily = fonts.cond,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(start = 14.dp),
    )
}

@Composable
private fun ProgressBar(pct: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(16.dp)
            .border(2.dp, NsRed)
            .background(NsRedSoft),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth((pct / 100f).coerceIn(0f, 1f))
                .background(NsRed),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(NsGold))
        }
    }
}

@Composable
private fun DashedRule() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(2.dp)
            .drawBehind {
                val dash = 8.dp.toPx()
                val gap = 6.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawRect(NsRed, topLeft = Offset(x, 0f), size = Size(dash, size.height))
                    x += dash + gap
                }
            },
    )
}

@Composable
private fun BracketCorner(modifier: Modifier, top: Boolean, left: Boolean) {
    Box(
        modifier
            .size(24.dp)
            .drawBehind {
                val t = 3.dp.toPx()
                val w = size.width
                val h = size.height
                // horizontal arm
                drawRect(NsGold, topLeft = Offset(0f, if (top) 0f else h - t), size = Size(w, t))
                // vertical arm
                drawRect(NsGold, topLeft = Offset(if (left) 0f else w - t, 0f), size = Size(t, h))
            },
    )
}
