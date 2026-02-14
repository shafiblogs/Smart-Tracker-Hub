package com.marsa.smarttrackerhub.domain


/**
 * Created by Muhammed Shafi on 14/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
/**
 * Chart statistics
 */
data class ChartStatistics(
    val totalMonths: Int,
    val totalTarget: Double,
    val totalAverage: Double,
    val monthsTargetMet: Int,
    val averageAchievementPercentage: Double,
    val monthsTargetMetPercentage: Double
)