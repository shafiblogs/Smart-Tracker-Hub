package com.marsa.smarttrackerhub.ui.screens.sale

import android.util.Log
import com.marsa.smarttrackerhub.data.entity.SummaryEntity
import java.text.SimpleDateFormat
import java.util.Locale

object TargetSaleCalculator {

    private const val INITIAL_TARGET = 1500.0
    private const val GROWTH_PERCENTAGE = 0.10 // 10%

    /**
     * Calculate target sale for all months in a shop based on previous months' actual average sales
     * Logic:
     * - 1st month: 1500
     * - 2nd month: 1st month's actual average + 10%
     * - 3rd month: average of first 2 months' actual averages + 10%
     * - 4th+ months: average of previous 3 months' actual averages + 10%
     */
    fun calculateTargetSalesForShop(summaries: List<SummaryEntity>): List<SummaryEntity> {
        if (summaries.isEmpty()) return emptyList()

        // Sort by month timestamp in ascending order (oldest first)
        val sortedSummaries = summaries.sortedBy { it.monthTimestamp }

        val updatedSummaries = mutableListOf<SummaryEntity>()

        sortedSummaries.forEachIndexed { index, summary ->
            val calculatedTarget = when (index) {
                0 -> {
                    // First month (earliest): use initial target
                    INITIAL_TARGET
                }
                1 -> {
                    // Second month: first month's actual average + 10%
                    val firstMonthActualAverage = updatedSummaries[0].averageSale ?: INITIAL_TARGET
                    firstMonthActualAverage * (1 + GROWTH_PERCENTAGE)
                }
                2 -> {
                    // Third month: average of first 2 months' actual averages + 10%
                    val avg1 = updatedSummaries[0].averageSale ?: INITIAL_TARGET
                    val avg2 = updatedSummaries[1].averageSale ?: INITIAL_TARGET
                    val averageOfTwo = (avg1 + avg2) / 2
                    averageOfTwo * (1 + GROWTH_PERCENTAGE)
                }
                else -> {
                    // 4th month onwards: average of previous 3 months' actual averages + 10%
                    val avg1 = updatedSummaries[index - 3].averageSale ?: INITIAL_TARGET
                    val avg2 = updatedSummaries[index - 2].averageSale ?: INITIAL_TARGET
                    val avg3 = updatedSummaries[index - 1].averageSale ?: INITIAL_TARGET
                    val averageOfThree = (avg1 + avg2 + avg3) / 3
                    averageOfThree * (1 + GROWTH_PERCENTAGE)
                }
            }

            // Create updated entity with calculated target
            updatedSummaries.add(
                summary.copy(targetSale = calculatedTarget)
            )
        }

        return updatedSummaries
    }

    /**
     * Parse month year string to timestamp for sorting
     * Handles format: "February - 2026", "January - 2026", etc.
     */
    fun parseMonthYearToTimestamp(monthYear: String): Long {
        return try {
            // Try with spaces around hyphen first
            val formatter = SimpleDateFormat("MMMM - yyyy", Locale.ENGLISH)
            formatter.parse(monthYear)?.time ?: run {
                // Fallback: try without spaces
                val formatter2 = SimpleDateFormat("MMMM-yyyy", Locale.ENGLISH)
                formatter2.parse(monthYear)?.time ?: 0L
            }
        } catch (e: Exception) {
            Log.e("TargetSaleCalculator", "Error parsing monthYear: $monthYear", e)
            0L
        }
    }
}