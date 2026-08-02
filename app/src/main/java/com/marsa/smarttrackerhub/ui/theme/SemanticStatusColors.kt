package com.marsa.smarttracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Fixed-meaning status colors used outside the Material3 color scheme (sync dots, achievement
 * indicators, positive/negative states). Shared vocabulary with SmartTracker & AccountsTracker:
 * a deeper shade for light backgrounds, a lighter/softer shade for dark backgrounds.
 *
 * success = positive/on-target (e.g. synced, target met)
 * danger  = negative/over (e.g. failed, under target)
 * warning = pending/at-risk (e.g. unsynced, near target)
 */
data class SemanticStatusColors(
    val success: Color,
    val danger: Color,
    val warning: Color
)

private val LightSemanticStatusColors = SemanticStatusColors(
    success = Color(0xFF15803D), // green-700
    danger  = Color(0xFFDC2626), // red-600
    warning = Color(0xFFEA580C)  // orange-600
)

private val DarkSemanticStatusColors = SemanticStatusColors(
    success = Color(0xFF4ADE80), // green-400
    danger  = Color(0xFFF87171), // red-400
    warning = Color(0xFFFB923C)  // orange-400
)

@Composable
fun semanticStatusColors(): SemanticStatusColors =
    if (isSystemInDarkTheme()) DarkSemanticStatusColors else LightSemanticStatusColors
