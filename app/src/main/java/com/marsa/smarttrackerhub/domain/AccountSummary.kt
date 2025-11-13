package com.marsa.smarttrackerhub.domain


/**
 * Created by Muhammed Shafi on 31/05/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

data class AccountSummary(
    val monthYear: String,
    val totalCollection: Double,
    val totalPurchases: Double,
    val totalExpenses: Double,
    val outstandingPayments: Double,
    val cashBalance: Double,
    val outstandingBalance: Double,
    val profitMargin: Double,
    val openingCashBalance: Double,
    val openingOutstandingBalance: Double
)
