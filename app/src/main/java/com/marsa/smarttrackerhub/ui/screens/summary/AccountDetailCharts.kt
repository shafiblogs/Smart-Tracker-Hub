package com.marsa.smarttrackerhub.ui.screens.summary

import androidx.compose.runtime.Composable
import com.marsa.smarttrackerhub.domain.AccountSummary
import com.marsa.smarttrackerhub.ui.components.DetailSectionCard
import com.marsa.smarttrackerhub.ui.screens.chart.AccountTrendChart
import com.marsa.smarttrackerhub.ui.screens.chart.CashFlowWaterfallChart
import com.marsa.smarttrackerhub.ui.screens.chart.MoneyAllocationBar
import com.marsa.smarttrackerhub.utils.formatLastUpdated

/**
 * The three graphical cards on the Account detail screen — cash-flow waterfall, where-the-money-
 * went allocation, and the multi-month profit trend. Split out of AccountDetailScreen.kt to keep
 * that file to scaffold + card assembly.
 */

@Composable
fun CashFlowCard(summary: AccountSummary, shopName: String, onShare: (() -> Unit)? = null) {
    DetailSectionCard(
        title = "Cash Flow",
        subtitle = shopName,
        caption = summary.lastUpdated.formatLastUpdated(),
        onShare = onShare
    ) {
        CashFlowWaterfallChart(
            openingCash = summary.openingCashBalance,
            collection = summary.totalCollection,
            purchase = summary.totalPurchases,
            expense = summary.totalExpenses,
            withdrawal = summary.withdrawal,
            closingCash = summary.cashBalance
        )
    }
}

@Composable
fun MoneyAllocationCard(summary: AccountSummary, shopName: String, onShare: (() -> Unit)? = null) {
    DetailSectionCard(
        title = "Where The Money Went",
        subtitle = shopName,
        caption = summary.lastUpdated.formatLastUpdated(),
        onShare = onShare
    ) {
        MoneyAllocationBar(
            collection = summary.totalCollection,
            purchase = summary.totalPurchases,
            expense = summary.totalExpenses,
            withdrawal = summary.withdrawal,
            provision = summary.provision,
            outstandingPayments = summary.outstandingPayments
        )
    }
}

/** Hides itself when fewer than 2 months are cached — nothing to trend yet. [history] is newest→oldest. */
@Composable
fun ProfitTrendCard(history: List<AccountSummary>, shopName: String, onShare: (() -> Unit)? = null) {
    if (history.size < 2) return
    DetailSectionCard(
        title = "Profit Trend",
        subtitle = shopName,
        caption = history.maxOf { it.lastUpdated }.formatLastUpdated(),
        onShare = onShare
    ) {
        AccountTrendChart(history = history.reversed())
    }
}
