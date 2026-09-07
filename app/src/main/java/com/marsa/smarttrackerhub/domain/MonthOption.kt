package com.marsa.smarttrackerhub.domain

import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Canonical identity for one calendar month, backed by [YearMonth] instead of hand-rolled
 * Calendar/SimpleDateFormat arithmetic. Replaces three near-identical types that used to be
 * declared separately: `MonthItem` in SaleScreenViewModel.kt, `MonthItem` in SummaryViewModel.kt,
 * and `MonthOption` in LogsViewModel.kt.
 *
 * Deliberately carries no single "document key". The sales/account summary docs and the logs
 * collection use two different Firestore key conventions for the same calendar month
 * ("MonthName - yyyy" vs "yyyy-MM") — see [SUMMARY_KEY_FORMATTER]'s doc — so each call site
 * derives its own key from [yearMonth] rather than this type picking one convention and being
 * wrong for the other.
 */
data class MonthOption(val yearMonth: YearMonth) {

    val displayName: String get() = yearMonth.format(DISPLAY_FORMATTER)

    companion object {
        private val DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

        /**
         * "MonthName - yyyy" — the Firestore document id / Room `monthYear` column for every
         * sales and account summary doc (see Helper.kt's getUpdatedMonthName, and
         * TargetSaleCalculator's parser for the same pattern). Always Locale.ENGLISH so the key
         * matches existing data regardless of device locale — a device set to Arabic must still
         * produce "March - 2026", not the Arabic month name.
         */
        val SUMMARY_KEY_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMMM - yyyy", Locale.ENGLISH)

        fun current(): MonthOption = MonthOption(YearMonth.now())

        /** Parses a summary-convention key ("MonthName - yyyy") back into a [MonthOption]. */
        fun fromSummaryKey(key: String): MonthOption? =
            runCatching { YearMonth.parse(key, SUMMARY_KEY_FORMATTER) }.getOrNull()?.let { MonthOption(it) }
    }
}
