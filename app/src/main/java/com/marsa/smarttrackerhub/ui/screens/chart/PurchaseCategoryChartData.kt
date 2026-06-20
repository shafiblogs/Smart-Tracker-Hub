package com.marsa.smarttrackerhub.ui.screens.chart

/**
 * Per-category data point for the Purchase Progress chart.
 *
 * [target] = previous month's totalAmount × 1.10.
 * When no previous month exists, [target] is 0.0 and [hasTarget] is false —
 * the bar is rendered without a reference baseline.
 *
 * Created by Muhammed Shafi on 24/03/2026.
 * Moro Hub
 */
data class PurchaseCategoryChartData(
    val categoryId: Int,
    val categoryName: String,
    val actual: Double,
    val target: Double
) {
    val hasTarget: Boolean = target > 0.0

    /** actual / target × 100. 0 when no target. */
    val achievementPercentage: Double =
        if (target > 0) (actual / target) * 100.0 else 0.0
}

/**
 * Aggregate statistics for the purchase progress card header.
 */
data class PurchaseChartStatistics(
    val totalActual: Double,
    val totalTarget: Double,
    val monthLabel: String,
    val categoriesOnTarget: Int,
    val totalCategories: Int
) {
    val achievementPercentage: Double =
        if (totalTarget > 0) (totalActual / totalTarget) * 100.0 else 0.0
}
