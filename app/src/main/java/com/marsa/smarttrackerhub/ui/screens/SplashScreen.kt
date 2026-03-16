package com.marsa.smarttrackerhub.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.marsa.smarttrackerhub.R
import com.marsa.smarttracker.ui.theme.sTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash screen matching the TrackerHub brand identity:
 *  - Animated app icon (launcher icon — always matches the installed icon)
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

    val context = LocalContext.current
    val iconBitmap = remember {
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)!!
        val bmp = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bmp.asImageBitmap()
    }

    // Dark mode colours — match DarkColorScheme in Color.kt
    val bgCenter   = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)   // DarkSurface / SurfaceWhite
    val bgEdge     = if (isDark) Color(0xFF0F172A) else Color(0xFFF5F6FA)   // DarkBackground / AppBackground
    val titleColor    = if (isDark) Color(0xFFE5E7EB) else Color(0xFF1C1F26)  // DarkTextPrimary / TextPrimary
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280) // DarkTextSecondary / TextSecondary

    // ── Make status bar + nav bar transparent so the background fills edge-to-edge ──
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
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
        launch { logoAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        launch { logoScale.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
        // Text fade-up after logo settles
        delay(400)
        launch { textAlpha.animateTo(1f, tween(500)) }
        launch { textOffsetY.animateTo(0f, tween(500, easing = FastOutSlowInEasing)) }
        // Navigate away
        delay(2400)
        onTimeout()
    }

    // ── Background: radial gradient, adapts to light/dark ────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(colors = listOf(bgCenter, bgEdge), radius = 1200f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── App Icon (always matches the installed launcher icon) ──────
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = BitmapPainter(iconBitmap),
                    contentDescription = "Tracker Hub",
                    modifier = Modifier.size(120.dp),
                    contentScale = ContentScale.Fit
                )
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
