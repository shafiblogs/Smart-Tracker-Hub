package com.marsa.smarttrackerhub.utils

/**
 * Single source for on-screen money *numeral* formatting — thousands separator, fixed decimals.
 * No currency mark here: that's the AED glyph, rendered by AedText (ui/components/AedText.kt)
 * in the UAESymbol font wherever an amount is shown in the UI. Use this directly only for
 * numerals that appear without a currency mark (e.g. inside a compound string already marked
 * once elsewhere, like a delta suffix).
 */
fun formatMoney(amount: Double, decimals: Int = 2): String =
    "%,.${decimals}f".format(amount)

/**
 * Compact numeral formatting for narrow layouts (e.g., three-across tiles).
 * 1,250,000 → "1.25M", 186,500 → "186.5K"
 */
fun formatMoneyCompact(amount: Double): String {
    return when {
        amount >= 1_000_000 -> {
            val millions = amount / 1_000_000
            "${"%.2f".format(millions).trimEnd('0').trimEnd('.')}M"
        }
        amount >= 1_000 -> {
            val thousands = amount / 1_000
            "${"%.1f".format(thousands).trimEnd('0').trimEnd('.')}K"
        }
        else -> "%,.0f".format(amount)
    }
}
