package com.marsa.smarttrackerhub.ui.screens.chart

import androidx.compose.ui.graphics.Color

/**
 * Single source for achievement status colors used across the charts and stat cards, so the whole
 * app tints "progress" the same way and a future palette tweak is a one-line change.
 * `error` is passed in from the caller's MaterialTheme.colorScheme so it stays theme-driven.
 */
/** Success / warning accents for chart status — one definition shared by every chart & stat card. */
val ChartSuccessGreen = Color(0xFF22C55E)
val ChartWarningAmber = Color(0xFFF59E0B)

/** Sales achievement vs target (target = 100%): >=100 green, 90-100 amber, else error. */
fun salesAchievementColor(percentage: Double, error: Color): Color = when {
    percentage >= 100 -> ChartSuccessGreen
    percentage >= 90 -> ChartWarningAmber
    else -> error
}

/** Purchase achievement vs budget: thresholds lowered 10pp (matches SmartTracker/AccountsTracker). */
fun purchaseAchievementColor(percentage: Double, error: Color): Color = when {
    percentage >= 90 -> ChartSuccessGreen
    percentage >= 75 -> ChartWarningAmber
    else -> error
}

/** Sales margin health (matches home UnifiedStatisticsCard): >=30 green, 10-30 amber, else error. */
fun salesMarginColor(percentage: Double, error: Color): Color = when {
    percentage >= 30 -> ChartSuccessGreen
    percentage >= 10 -> ChartWarningAmber
    else -> error
}
