package com.marsa.smarttrackerhub.domain

/**
 * What a month/period picker resolved to. Replaces the old `MonthRange` sealed class, which
 * hardcoded three separate "single month" subclasses (CurrentMonth/PreviousMonth/
 * PreviousPreviousMonth) for what is structurally the same case — a single [MonthOption] — plus
 * two range subclasses. A free month picker replaces the three hardcoded relative months, so
 * there's now exactly one "single month" case; screens that never offer a range (Sales Detail,
 * Account Detail, Logs) only ever construct [Month].
 */
sealed class MonthSelection {

    abstract val displayName: String

    data class Month(val option: MonthOption) : MonthSelection() {
        override val displayName: String get() = option.displayName
    }

    /**
     * The [monthCount] completed calendar months immediately before the current one — the
     * current (in-progress) month is always excluded, e.g. "Last 3 Months" from March means
     * December–February, not December–March.
     */
    data class TrailingRange(val monthCount: Int) : MonthSelection() {
        override val displayName: String get() = "Last $monthCount Months"
    }

    companion object {
        fun currentMonth(): Month = Month(MonthOption.current())
    }
}
