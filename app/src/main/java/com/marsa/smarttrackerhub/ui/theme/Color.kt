package com.marsa.smarttracker.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Brand palette (matches TH launch icon) ────────────────────────────────

// Orange brand colours
private val BrandOrange      = Color(0xFFFF7A00)
private val BrandOrangeDark  = Color(0xFFE56700)
private val BrandOrangeLight = Color(0xFFFFB266)

// Blue brand colours
private val BrandBlue      = Color(0xFF2F6BFF)
private val BrandBlueDark  = Color(0xFF1E4ED8)
private val BrandBlueLight = Color(0xFF6EA8FF)

// Light-theme neutrals
private val AppBackground = Color(0xFFF5F6FA)
private val SurfaceWhite  = Color(0xFFFFFFFF)
private val CardSurface   = Color(0xFFF0F2F7)
private val TextPrimary   = Color(0xFF1C1F26)
private val TextSecondary = Color(0xFF6B7280)
private val OutlineLight  = Color(0xFFD1D5DB)

// Light-theme status
private val SuccessGreen  = Color(0xFF22C55E)
private val WarningAmber  = Color(0xFFF59E0B)
private val ErrorRed      = Color(0xFFEF4444)
private val ErrorRedDark  = Color(0xFFB91C1C)

// Dark-theme brand
private val BrandBlueDarkTheme   = Color(0xFF6EA8FF)
private val BrandOrangeDarkTheme = Color(0xFFFF9A3C)

// Dark-theme neutrals
private val DarkBackground  = Color(0xFF0F172A)
private val DarkSurface     = Color(0xFF1E293B)
private val DarkCardSurface = Color(0xFF273449)
private val DarkTextPrimary = Color(0xFFE5E7EB)
private val DarkTextSecondary = Color(0xFF94A3B8)
private val DarkOutline     = Color(0xFF334155)

// Dark-theme status
private val DarkSuccessGreen = Color(0xFF4ADE80)
private val DarkWarningAmber = Color(0xFFFBBF24)
private val DarkErrorRed     = Color(0xFFF87171)
private val DarkErrorDark    = Color(0xFF7F1D1D)

// ── Light colour scheme ───────────────────────────────────────────────────

val LightColorScheme = lightColorScheme(
    // Blue is the dominant/primary brand colour (used for buttons, FABs, active states)
    primary            = BrandBlue,
    onPrimary          = Color.White,
    primaryContainer   = BrandBlueLight.copy(alpha = 0.25f),  // very light blue tint for containers
    onPrimaryContainer = BrandBlueDark,

    // Orange is the secondary/accent brand colour (chips, tags, highlights)
    secondary            = BrandOrange,
    onSecondary          = Color.White,
    secondaryContainer   = BrandOrangeLight.copy(alpha = 0.25f),
    onSecondaryContainer = BrandOrangeDark,

    // Tertiary — success/positive states (green)
    tertiary            = SuccessGreen,
    onTertiary          = Color.White,
    tertiaryContainer   = SuccessGreen.copy(alpha = 0.2f),
    onTertiaryContainer = Color(0xFF165E2D),

    background         = AppBackground,
    onBackground       = TextPrimary,

    surface            = SurfaceWhite,
    onSurface          = TextPrimary,
    surfaceVariant     = CardSurface,
    onSurfaceVariant   = TextSecondary,
    outline            = OutlineLight,
    outlineVariant     = Color(0xFFE5E7EB),

    error              = ErrorRed,
    onError            = Color.White,
    errorContainer     = Color(0xFFFFE4E4),
    onErrorContainer   = ErrorRedDark
)

// ── Dark colour scheme ────────────────────────────────────────────────────

val DarkColorScheme = darkColorScheme(
    primary            = BrandBlueDarkTheme,
    onPrimary          = DarkBackground,
    primaryContainer   = Color(0xFF1E3A6E),
    onPrimaryContainer = BrandBlueDarkTheme,

    secondary            = BrandOrangeDarkTheme,
    onSecondary          = DarkBackground,
    secondaryContainer   = Color(0xFF5C3200),
    onSecondaryContainer = BrandOrangeDarkTheme,

    tertiary            = DarkSuccessGreen,
    onTertiary          = DarkBackground,
    tertiaryContainer   = DarkSuccessGreen.copy(alpha = 0.2f),
    onTertiaryContainer = DarkSuccessGreen,

    background         = DarkBackground,
    onBackground       = DarkTextPrimary,

    surface            = DarkSurface,
    onSurface          = DarkTextPrimary,
    surfaceVariant     = DarkCardSurface,
    onSurfaceVariant   = DarkTextSecondary,
    outline            = DarkOutline,
    outlineVariant     = Color(0xFF475569),  // Lighter for visibility in dark mode

    error              = DarkErrorRed,
    onError            = DarkErrorDark,
    errorContainer     = Color(0xFF450A0A),
    onErrorContainer   = DarkErrorRed
)
