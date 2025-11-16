package com.marsa.smarttrackerhub.domain


/**
 * Created by Muhammed Shafi on 31/05/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

data class AccountSummary(
    val monthYear: String = "",
    val totalCollection: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val outstandingPayments: Double = 0.0,
    val cashBalance: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val grossProfit: Double = 0.0,
    val grossMargin: Double = 0.0,
    val netProfit: Double = 0.0,
    val netProfitMargin: Double = 0.0,
    val openingCashBalance: Double = 0.0,
    val openingOutstandingBalance: Double = 0.0
)
