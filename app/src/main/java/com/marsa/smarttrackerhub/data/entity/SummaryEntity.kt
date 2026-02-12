package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.marsa.smarttrackerhub.domain.MonthlySummary
import com.marsa.smarttrackerhub.ui.screens.home.TargetSaleCalculator


/**
 * Created by Muhammed Shafi on 25/06/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Entity(tableName = "summary")
data class SummaryEntity(
    @PrimaryKey
    val id: String, // shopId + monthId combination
    val shopId: String,
    val monthId: String,
    val monthYear: String,
    val monthTimestamp: Long, // For proper sorting
    val openingCashBalance: Double,
    val cashBalance: Double,
    val openingAccountBalance: Double,
    val accountBalance: Double,
    val openingCreditBalance: Double,
    val creditSaleBalance: Double,
    val averageSale: Double?, // Actual average sale from server
    val targetSale: Double, // Calculated target for this month
    val totalSales: Double,
    val totalPurchases: Double,
    val totalExpenses: Double,
    val totalCashIn: Double,
    val totalCashOut: Double,
    val totalCreditSale: Double,
    val creditSalePayment: Double,
    val lastUpdated: Long // Timestamp for cache expiry
)

fun SummaryEntity.toDomain(): MonthlySummary {
    return MonthlySummary(
        monthYear = monthYear,
        openingCashBalance = openingCashBalance,
        cashBalance = cashBalance,
        openingAccountBalance = openingAccountBalance,
        accountBalance = accountBalance,
        openingCreditBalance = openingCreditBalance,
        creditSaleBalance = creditSaleBalance,
        averageSale = averageSale, // Keep the actual average from server
        totalSales = totalSales,
        totalPurchases = totalPurchases,
        totalExpenses = totalExpenses,
        totalCashIn = totalCashIn,
        totalCashOut = totalCashOut,
        totalCreditSale = totalCreditSale,
        creditSalePayment = creditSalePayment
    )
}

fun MonthlySummary.toEntity(shopId: String, monthId: String): SummaryEntity {
    val monthTimestamp = TargetSaleCalculator.parseMonthYearToTimestamp(monthYear)

    return SummaryEntity(
        id = "$shopId-$monthId",
        shopId = shopId,
        monthId = monthId,
        monthYear = monthYear,
        monthTimestamp = monthTimestamp,
        openingCashBalance = openingCashBalance,
        cashBalance = cashBalance,
        openingAccountBalance = openingAccountBalance,
        accountBalance = accountBalance,
        openingCreditBalance = openingCreditBalance,
        creditSaleBalance = creditSaleBalance,
        averageSale = averageSale, // Store actual average from server
        targetSale = 0.0, // Will be calculated by TargetSaleCalculator
        totalSales = totalSales,
        totalPurchases = totalPurchases,
        totalExpenses = totalExpenses,
        totalCashIn = totalCashIn,
        totalCashOut = totalCashOut,
        totalCreditSale = totalCreditSale,
        creditSalePayment = creditSalePayment,
        lastUpdated = System.currentTimeMillis()
    )
}