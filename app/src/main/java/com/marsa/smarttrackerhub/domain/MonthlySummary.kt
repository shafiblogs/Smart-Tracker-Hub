package com.marsa.smarttrackerhub.domain


/**
 * Created by Muhammed Shafi on 31/05/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

data class MonthlySummary(
    val shopId: String = "",
    val monthYear: String = "",
    val totalSales: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val cashBalance: Double = 0.0,
    val accountBalance: Double = 0.0,
    val creditSaleBalance: Double = 0.0,
    val creditSalePayment: Double = 0.0,
    val openingCashBalance: Double = 0.0,
    val openingCreditBalance: Double = 0.0,
    val openingAccountBalance: Double = 0.0,
    val updatedDate: String = ""
)
