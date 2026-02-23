package com.marsa.smarttrackerhub.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsa.smarttracker.ui.theme.sTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash screen matching the TrackerHub brand identity:
 *  - Animated TH logo (orange T + chart, blue H)
 *  - "Tracker Hub" bold title
 *  - "Sales & Shop Management" subtitle
 *
 * Created by Muhammed Shafi on 22/06/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun SplashScreen(onTimeout: () -> Unit) {

    val isDark = isSystemInDarkTheme()

    // Dark mode colours — match DarkColorScheme in Color.kt
    val bgCenter  = if (isDark) Color(0xFF1A1F1D) else Color(0xFFFFFFFF)
    val bgEdge    = if (isDark) Color(0xFF101412) else Color(0xFFF0EFEB)
    val titleColor   = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
    val subtitleColor = if (isDark) Color(0xFF8E918F) else Color(0xFF404946)

    // ── Make status bar + nav bar transparent so the background fills edge-to-edge ──
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                // Light mode → dark icons; Dark mode → light icons
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    // ── Animation values ──────────────────────────────────────────────────
    val logoScale   = remember { Animatable(0.6f) }
    val logoAlpha   = remember { Animatable(0f) }
    val textAlpha   = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(24f) }

    LaunchedEffect(Unit) {
        // Logo pop-in
        launch {
            logoAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        }
        // Text fade-up after logo
        delay(400)
        launch {
            textAlpha.animateTo(1f, tween(500))
        }
        launch {
            textOffsetY.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
        }
        // Navigate
        delay(2400)
        onTimeout()
    }

    // ── Background: adapts to light/dark theme ────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(bgCenter, bgEdge),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── TH Logo Icon ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(130.dp)) {
                    drawTHLogo(this, isDark)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── "Tracker Hub" title ────────────────────────────────────────
            Text(
                text = "Tracker Hub",
                style = sTypography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = titleColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .padding(top = (textOffsetY.value).dp)
            )

            Spacer(Modifier.height(6.dp))

            // ── Subtitle ───────────────────────────────────────────────────
            Text(
                text = "Sales & Shop Management",
                style = sTypography.bodyMedium.copy(
                    fontSize = 14.sp,
                    letterSpacing = 0.2.sp
                ),
                color = subtitleColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .padding(top = (textOffsetY.value * 0.5f).dp)
            )
        }
    }
}

// ── TH Logo Canvas Drawing ────────────────────────────────────────────────────

private fun drawTHLogo(scope: DrawScope, isDark: Boolean = false) {
    val S = scope.size.width
    scope.run {

        // ── Rounded card shadow ───────────────────────────────────────────
        // Light mode: dark shadow; Dark mode: subtle lighter glow
        val shadowColor = if (isDark) Color(0x28FFFFFF) else Color(0x18000000)
        drawRoundRect(
            color = shadowColor,
            topLeft = Offset(S * 0.04f, S * 0.06f),
            size = Size(S * 0.92f, S * 0.92f),
            cornerRadius = CornerRadius(S * 0.22f),
        )

        // ── Card background — white in light, dark surface in dark mode ───
        val cardTop    = if (isDark) Color(0xFF232B28) else Color(0xFFFFFFFF)
        val cardBottom = if (isDark) Color(0xFF1A211E) else Color(0xFFF4F4F6)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(cardTop, cardBottom),
                startY = 0f,
                endY = S
            ),
            topLeft = Offset(S * 0.02f, S * 0.02f),
            size = Size(S * 0.96f, S * 0.96f),
            cornerRadius = CornerRadius(S * 0.22f),
        )

        // ── Layout ────────────────────────────────────────────────────────
        val pad    = S * 0.08f
        val lx0    = pad
        val lx1    = S - pad
        val lw     = lx1 - lx0

        val ly0    = S * 0.42f          // top of letters
        val ly1    = S * 0.88f          // bottom of letters
        val lh     = ly1 - ly0
        val stroke = lw * 0.145f        // letter stroke thickness

        val tW  = lw * 0.42f            // T width
        val hW  = lw * 0.42f            // H width
        val hX0 = lx0 + tW + lw * 0.16f

        // ── Letter T — orange→red gradient ───────────────────────────────
        val tGrad = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFAA00), Color(0xFFE03010)),
            startY = ly0, endY = ly1
        )
        // Crossbar
        drawRect(brush = tGrad, topLeft = Offset(lx0, ly0), size = Size(tW, stroke))
        // Stem
        val stemX = lx0 + (tW - stroke) / 2f
        drawRect(brush = tGrad, topLeft = Offset(stemX, ly0 + stroke), size = Size(stroke, lh - stroke))

        // ── Letter H — cyan→blue gradient ────────────────────────────────
        val hGrad = Brush.verticalGradient(
            colors = listOf(Color(0xFF60CCFF), Color(0xFF1A6AE8)),
            startY = ly0, endY = ly1
        )
        // Left stem
        drawRect(brush = hGrad, topLeft = Offset(hX0, ly0), size = Size(stroke, lh))
        // Right stem
        drawRect(brush = hGrad, topLeft = Offset(hX0 + hW - stroke, ly0), size = Size(stroke, lh))
        // Crossbar
        val cbY0 = ly0 + lh * 0.38f
        val cbY1 = ly0 + lh * 0.60f
        drawRect(brush = hGrad, topLeft = Offset(hX0 + stroke, cbY0), size = Size(hW - stroke * 2f, cbY1 - cbY0))

        // ── Bar chart above T ─────────────────────────────────────────────
        val chartY1 = ly0 - S * 0.02f
        val chartY0 = S * 0.08f
        val chartH  = chartY1 - chartY0
        val barAreaX0 = lx0 + tW * 0.05f
        val barAreaW  = tW * 0.90f
        val nBars = 3
        val barGap = barAreaW * 0.08f
        val barW   = (barAreaW - barGap * (nBars - 1)) / nBars
        val barHeights = listOf(0.42f, 0.70f, 1.00f)
        val barGrad = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFB520), Color(0xFFE05010)),
            startY = chartY0, endY = chartY1
        )

        barHeights.forEachIndexed { i, rel ->
            val bx0 = barAreaX0 + i * (barW + barGap)
            val bh  = chartH * rel
            drawRect(
                brush = barGrad,
                topLeft = Offset(bx0, chartY1 - bh),
                size = Size(barW, bh)
            )
        }

        // ── Trend line + dots on bar tops ────────────────────────────────
        val pts = barHeights.mapIndexed { i, rel ->
            val bx0 = barAreaX0 + i * (barW + barGap)
            Offset(bx0 + barW / 2f, chartY1 - chartH * rel - S * 0.012f)
        }

        val lineColor = Color(0xFFFFAA00)
        val lineStroke = S * 0.030f
        val dotR = S * 0.038f

        for (i in 0 until pts.size - 1) {
            drawLine(
                color = lineColor,
                start = pts[i],
                end = pts[i + 1],
                strokeWidth = lineStroke,
                cap = StrokeCap.Round
            )
        }
        pts.forEach { pt ->
            drawCircle(color = lineColor, radius = dotR, center = pt)
            drawCircle(color = Color.White, radius = dotR * 0.48f, center = pt)
        }

        // ── Drop shadow under letters (subtle) ────────────────────────────
        // Already handled by card shadow above
    }
}
