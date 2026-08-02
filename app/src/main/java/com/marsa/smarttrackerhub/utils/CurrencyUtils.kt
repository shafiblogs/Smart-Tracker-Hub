package com.marsa.smarttrackerhub.utils

/**
 * Single source for on-screen money formatting — the dirham glyph + thousands separator,
 * matching SmartTracker & AccountsTracker. Use everywhere instead of inline "Đ%.2f".
 */
fun formatMoney(amount: Double, decimals: Int = 2): String =
    "Đ${"%,.${decimals}f".format(amount)}"
