package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.marsa.smarttrackerhub.domain.MonthlySummary
import com.marsa.smarttrackerhub.ui.screens.sale.TargetSaleCalculator

@Entity(tableName = "summary")
data class SummaryEntity(
    @PrimaryKey
    val id: String,
    val shopId: String,
    val monthId: String,
    val monthYear: String,
    val monthTimestamp: Long,
    val openingCashBalance: Double,
    val cashBalance: Double,
    val openingAccountBalance: Double,
    val accountBalance: Double,
    val openingCreditBalance: Double,
    val creditSaleBalance: Double,
    val averageSale: Double?,
    val targetSale: Double,
    val totalSales: Double,
    val totalPurchases: Double,
    val totalExpenses: Double,
    val totalCashIn: Double,
    val totalCashOut: Double,
    val totalCreditSale: Double,
    val creditSalePayment: Double,
    val lastUpdated: Long
)

fun SummaryEntity.toDomain(): MonthlySummary {
    return MonthlySummary(
        shopId = shopId,
        monthYear = monthYear,
        openingCashBalance = openingCashBalance,
        cashBalance = cashBalance,
        openingAccountBalance = openingAccountBalance,
        accountBalance = accountBalance,
        openingCreditBalance = openingCreditBalance,
        creditSaleBalance = creditSaleBalance,
        averageSale = averageSale,
        totalSales = totalSales,
        totalPurchases = totalPurchases,
        totalExpenses = totalExpenses,
        totalCashIn = totalCashIn,
        totalCashOut = totalCashOut,
        totalCreditSale = totalCreditSale,
        creditSalePayment = creditSalePayment,
        updatedDate = "", // Can be derived from lastUpdated if needed
        lastUpdated = lastUpdated // NEW: Pass the timestamp
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
        averageSale = averageSale,
        targetSale = 0.0,
        totalSales = totalSales,
        totalPurchases = totalPurchases,
        totalExpenses = totalExpenses,
        totalCashIn = totalCashIn,
        totalCashOut = totalCashOut,
        totalCreditSale = totalCreditSale,
        creditSalePayment = creditSalePayment,
        lastUpdated = if (lastUpdated > 0) lastUpdated else System.currentTimeMillis() // Use existing or create new
    )
}