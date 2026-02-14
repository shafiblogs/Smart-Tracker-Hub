package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.marsa.smarttrackerhub.domain.AccountSummary
import com.marsa.smarttrackerhub.ui.screens.sale.TargetSaleCalculator

@Entity(tableName = "account_summary")
data class AccountSummaryEntity(
    @PrimaryKey
    val id: String, // shopId + monthId combination
    val shopId: String,
    val monthId: String,
    val monthYear: String,
    val monthTimestamp: Long, // For proper sorting
    val totalCollection: Double,
    val totalPurchases: Double,
    val totalExpenses: Double,
    val outstandingPayments: Double,
    val cashBalance: Double,
    val outstandingBalance: Double,
    val grossProfit: Double,
    val grossMargin: Double,
    val netProfit: Double,
    val netProfitMargin: Double,
    val openingCashBalance: Double,
    val openingOutstandingBalance: Double,
    val lastUpdated: Long // Timestamp for cache tracking
)

fun AccountSummaryEntity.toDomain(): AccountSummary {
    return AccountSummary(
        monthYear = monthYear,
        totalCollection = totalCollection,
        totalPurchases = totalPurchases,
        totalExpenses = totalExpenses,
        outstandingPayments = outstandingPayments,
        cashBalance = cashBalance,
        outstandingBalance = outstandingBalance,
        grossProfit = grossProfit,
        grossMargin = grossMargin,
        netProfit = netProfit,
        netProfitMargin = netProfitMargin,
        openingCashBalance = openingCashBalance,
        openingOutstandingBalance = openingOutstandingBalance,
        lastUpdated = lastUpdated
    )
}

fun AccountSummary.toEntity(shopId: String, monthId: String): AccountSummaryEntity {
    val monthTimestamp = TargetSaleCalculator.parseMonthYearToTimestamp(monthYear)

    return AccountSummaryEntity(
        id = "$shopId-$monthId",
        shopId = shopId,
        monthId = monthId,
        monthYear = monthYear,
        monthTimestamp = monthTimestamp,
        totalCollection = totalCollection,
        totalPurchases = totalPurchases,
        totalExpenses = totalExpenses,
        outstandingPayments = outstandingPayments,
        cashBalance = cashBalance,
        outstandingBalance = outstandingBalance,
        grossProfit = grossProfit,
        grossMargin = grossMargin,
        netProfit = netProfit,
        netProfitMargin = netProfitMargin,
        openingCashBalance = openingCashBalance,
        openingOutstandingBalance = openingOutstandingBalance,
        lastUpdated = if (lastUpdated > 0) lastUpdated else System.currentTimeMillis()
    )
}