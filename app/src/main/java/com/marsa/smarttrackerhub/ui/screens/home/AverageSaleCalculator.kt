package com.marsa.smarttrackerhub.ui.screens.home

import com.marsa.smarttrackerhub.data.entity.SummaryEntity
import java.text.SimpleDateFormat
import java.util.Locale

object AverageSaleCalculator {

    private const val INITIAL_AVERAGE = 1500.0
    private const val GROWTH_PERCENTAGE = 0.10 // 10%

    /**
     * Calculate average sale for all months in a shop
     * Returns updated list of entities with calculated average sales
     */
    fun calculateAverageSalesForShop(summaries: List<SummaryEntity>): List<SummaryEntity> {
        if (summaries.isEmpty()) return emptyList()

        // Sort by month timestamp in ascending order (oldest first)
        val sortedSummaries = summaries.sortedBy { it.monthTimestamp }

        val updatedSummaries = mutableListOf<SummaryEntity>()

        sortedSummaries.forEachIndexed { index, summary ->
            val calculatedAverage = when (index) {
                0 -> {
                    // First month: use initial average
                    INITIAL_AVERAGE
                }
                1 -> {
                    // Second month: first month's average + 10%
                    val firstMonthAverage = updatedSummaries[0].calculatedAverageSale
                    firstMonthAverage * (1 + GROWTH_PERCENTAGE)
                }
                2 -> {
                    // Third month: average of first 2 months + 10%
                    val avg1 = updatedSummaries[0].calculatedAverageSale
                    val avg2 = updatedSummaries[1].calculatedAverageSale
                    val averageOfTwo = (avg1 + avg2) / 2
                    averageOfTwo * (1 + GROWTH_PERCENTAGE)
                }
                else -> {
                    // 4th month onwards: average of previous 3 months + 10%
                    val avg1 = updatedSummaries[index - 3].calculatedAverageSale
                    val avg2 = updatedSummaries[index - 2].calculatedAverageSale
                    val avg3 = updatedSummaries[index - 1].calculatedAverageSale
                    val averageOfThree = (avg1 + avg2 + avg3) / 3
                    averageOfThree * (1 + GROWTH_PERCENTAGE)
                }
            }

            // Create updated entity with calculated average
            updatedSummaries.add(
                summary.copy(calculatedAverageSale = calculatedAverage)
            )
        }

        return updatedSummaries
    }

    /**
     * Parse month year string to timestamp for sorting
     */
    fun parseMonthYearToTimestamp(monthYear: String): Long {
        return try {
            val formatter = SimpleDateFormat("MMMM - yyyy", Locale.ENGLISH)
            formatter.parse(monthYear)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}