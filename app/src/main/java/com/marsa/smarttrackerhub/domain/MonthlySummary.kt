package com.marsa.smarttrackerhub.domain


/**
 * Created by Muhammed Shafi on 31/05/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

data class MonthlySummary(
    val monthYear: String,
    val totalSales: Double,
    val totalPurchases: Double,
    val totalExpenses: Double,
    val cashBalance: Double,
    val accountBalance: Double,
    val creditSaleBalance: Double,
    val creditSalePayment: Double,
    val openingCashBalance: Double,
    val openingCreditBalance: Double,
    val openingAccountBalance: Double,
)