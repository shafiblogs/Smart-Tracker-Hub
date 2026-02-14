package com.marsa.smarttrackerhub.ui.screens.chart

/**
 * Data model for monthly sales chart
 */
data class MonthlyChartData(
    val monthYear: String,        // Full month-year (e.g., "August - 2025")
    val monthShortName: String,   // Short name for display (e.g., "Aug")
    val targetSale: Double,       // Target for this month
    val averageSale: Double,      // Actual average sale achieved
    val isTargetMet: Boolean = averageSale >= targetSale
)

/**
 * Touch point information for tooltips
 */
data class ChartTouchInfo(
    val monthYear: String,
    val targetSale: Double,
    val averageSale: Double,
    val isTargetMet: Boolean,
    val difference: Double = averageSale - targetSale,
    val achievementPercentage: Double = if (targetSale > 0) (averageSale / targetSale) * 100.0 else 0.0
)
