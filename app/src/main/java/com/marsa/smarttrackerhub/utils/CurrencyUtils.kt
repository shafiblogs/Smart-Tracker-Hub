package com.marsa.smarttrackerhub.utils

/**
 * Single source for on-screen money formatting — AED + thousands separator.
 * Use everywhere instead of inline "AED %".
 */
private const val CURRENCY_SYMBOL = "AED "

fun formatMoney(amount: Double, decimals: Int = 2): String =
    "$CURRENCY_SYMBOL${"%,.${decimals}f".format(amount)}"

/**
 * Compact currency formatting for narrow layouts (e.g., three-across tiles).
 * 1,250,000 → "AED 1.25M", 186,500 → "AED 186.5K"
 */
fun formatMoneyCompact(amount: Double): String {
    return when {
        amount >= 1_000_000 -> {
            val millions = amount / 1_000_000
            "$CURRENCY_SYMBOL${"%.2f".format(millions).trimEnd('0').trimEnd('.')}M"
        }
        amount >= 1_000 -> {
            val thousands = amount / 1_000
            "$CURRENCY_SYMBOL${"%.1f".format(thousands).trimEnd('0').trimEnd('.')}K"
        }
        else -> "$CURRENCY_SYMBOL${"%,.0f".format(amount)}"
    }
}
