package com.marsa.smarttrackerhub.domain

/**
 * All domain/projection models for the investment module.
 *
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

/**
 * Used in Shop Investment Dashboard — one row per investor in a shop.
 * totalPaid = SUM of all InvestmentTransaction.amount for this ShopInvestor.
 */
data class ShopInvestorSummary(
    val shopInvestorId: Int,
    val investorId: Int,
    val investorName: String,
    val sharePercentage: Double,
    val totalPaid: Double,      // SUM(investment_transaction.amount)
    val status: String,
    val joinedDate: Long
)

/**
 * Used in Investor Portfolio screen — one row per shop this investor is in.
 * totalPaid = SUM of all InvestmentTransaction.amount for this investor in this shop.
 */
data class InvestorShopSummary(
    val shopInvestorId: Int,
    val shopId: Int,
    val shopName: String,
    val shopAddress: String,
    val sharePercentage: Double,
    val totalPaid: Double,      // SUM(investment_transaction.amount)
    val status: String,
    val joinedDate: Long
)

/**
 * Used in the Phase/Transaction breakdown list — one row per transaction.
 */
data class PhaseTransactionDetail(
    val transactionId: Int,
    val shopInvestorId: Int,
    val investorId: Int,
    val investorName: String,
    val phase: String,
    val amount: Double,
    val transactionDate: Long,
    val note: String
)

/**
 * Computed row for Year-End Settlement screen — not stored, computed live.
 * balanceAmount > 0 → others owe this investor
 * balanceAmount < 0 → this investor owes others
 */
data class InvestorSettlementRow(
    val investorId: Int,
    val investorName: String,
    val sharePercentage: Double,
    val fairShareAmount: Double,    // sharePercentage/100 × totalInvested
    val actualPaidAmount: Double,   // SUM of their transactions
    val balanceAmount: Double       // actualPaid - fairShare
)
