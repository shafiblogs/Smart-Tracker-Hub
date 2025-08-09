package com.marsa.smarttracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun SmartTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamic =
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            val fallback = if (darkTheme) DarkColorScheme else LightColorScheme

            dynamic.copy(
                background = fallback.background,
                onBackground = fallback.onBackground,
                surface = fallback.surface,
                onSurface = fallback.onSurface,
                onSurfaceVariant = fallback.onSurfaceVariant,
                outline = fallback.outline
            )
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = sTypography,
        content = content
    )
}

