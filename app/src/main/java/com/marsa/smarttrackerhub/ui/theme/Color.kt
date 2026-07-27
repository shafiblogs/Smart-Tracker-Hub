package com.marsa.smarttracker.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Unified warm-neutral + teal-green palette — shared with SmartTracker & AccountsTracker so all
// three apps read as one product. Warm dark surfaces (never pure black / cool navy).

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006A60),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF74F8E3),
    onPrimaryContainer = Color(0xFF00201C),

    secondary = Color(0xFF4B635E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE8E1),
    onSecondaryContainer = Color(0xFF071F1B),

    tertiary = Color(0xFF406375),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC4E8FD),
    onTertiaryContainer = Color(0xFF001F2B),

    background = Color(0xFFF0EFEB),
    onBackground = Color(0xFF1D1B20),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1B20),
    onSurfaceVariant = Color(0xFF404946),
    outline = Color(0xFF707976),
    outlineVariant = Color(0xFFC4CDC9),

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF54DBC7),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFF74F8E3),

    secondary = Color(0xFFB1CCC5),
    onSecondary = Color(0xFF1D3530),
    secondaryContainer = Color(0xFF344B46),
    onSecondaryContainer = Color(0xFFCDE8E1),

    tertiary = Color(0xFFA8CCDF),
    onTertiary = Color(0xFF0C3545),
    tertiaryContainer = Color(0xFF274B5C),
    onTertiaryContainer = Color(0xFFC4E8FD),

    // Warm-neutral dark surfaces (not green-tinted, never pure black) — cards a notch lighter
    // than the background for separation.
    background = Color(0xFF1A1917),
    onBackground = Color(0xFFEAE7E1),

    surface = Color(0xFF302E2A),
    onSurface = Color(0xFFEAE7E1),
    surfaceVariant = Color(0xFF3A3732),
    onSurfaceVariant = Color(0xFFBCB7AE),

    surfaceContainerLowest = Color(0xFF151412),
    surfaceContainerLow = Color(0xFF232220),
    surfaceContainer = Color(0xFF2A2825),
    surfaceContainerHigh = Color(0xFF33302C),
    surfaceContainerHighest = Color(0xFF3E3B36),

    outline = Color(0xFF9C968C),
    outlineVariant = Color(0xFF4F4B44),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)
