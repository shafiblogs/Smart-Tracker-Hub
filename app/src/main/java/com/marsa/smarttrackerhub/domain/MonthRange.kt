package com.marsa.smarttrackerhub.domain

import java.text.SimpleDateFormat
import java.util.*

/**
 * Represents different time period options for viewing sales data
 */
sealed class MonthRange(val displayName: String, val months: Int) {

    data class CurrentMonth(val monthName: String) : MonthRange(monthName, 1)
    data class PreviousMonth(val monthName: String) : MonthRange(monthName, 1)
    object Last3Months : MonthRange("Last 3 Months", 3)
    object Last6Months : MonthRange("Last 6 Months", 6)

    companion object {
        /**
         * Generates available period options based on current date
         */
        fun getAvailableRanges(): List<MonthRange> {
            val calendar = Calendar.getInstance()
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

            // Current month
            val currentMonthName = monthFormat.format(calendar.time)

            // Previous month
            calendar.add(Calendar.MONTH, -1)
            val previousMonthName = monthFormat.format(calendar.time)

            return listOf(
                CurrentMonth(currentMonthName),
                PreviousMonth(previousMonthName),
                Last3Months,
                Last6Months
            )
        }
    }
}