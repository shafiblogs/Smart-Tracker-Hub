package com.marsa.smarttrackerhub.ui.screens.summary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marsa.smarttrackerhub.domain.AccountSummary
import com.marsa.smarttrackerhub.ui.components.DetailSectionCard
import com.marsa.smarttrackerhub.ui.components.SegmentedControl
import com.marsa.smarttrackerhub.ui.screens.chart.AccountTrendChart
import com.marsa.smarttrackerhub.ui.screens.chart.CashFlowWaterfallChart
import com.marsa.smarttrackerhub.ui.screens.chart.MoneyAllocationBar
import com.marsa.smarttrackerhub.utils.formatLastUpdated
import com.marsa.smarttracker.ui.theme.spaceMd

/**
 * The graphical/tabular cards on the Account detail screen — the "where the money moved" card
 * (cash-flow waterfall / allocation split / statement figures, one lens at a time) and the
 * multi-month profit trend. Split out of AccountDetailScreen.kt to keep that file to scaffold +
 * card assembly.
 */

/**
 * Statement, Cash Flow and Where The Money Went used to be three separate cards describing the
 * same outflow data three ways. Folded into one card with a segmented control so a reader picks
 * a lens instead of scrolling three card-lengths to learn one thing three times (D9).
 */
enum class MoneyMovedView(val label: String) { FLOW("Flow"), SPLIT("Split"), FIGURES("Figures") }

@Composable
fun MoneyMovedCard(
    summary: AccountSummary,
    shopName: String,
    selected: MoneyMovedView,
    onSelectedChange: (MoneyMovedView) -> Unit,
    onShare: (() -> Unit)? = null
) {
    DetailSectionCard(
        title = "Where the money moved",
        subtitle = shopName,
        caption = summary.lastUpdated.formatLastUpdated(),
        onShare = onShare
    ) {
        Column {
            SegmentedControl(
                options = MoneyMovedView.entries.map { it.label },
                selectedIndex = MoneyMovedView.entries.indexOf(selected),
                onSelect = { index -> onSelectedChange(MoneyMovedView.entries[index]) }
            )
            Spacer(modifier = Modifier.height(spaceMd))
            when (selected) {
                MoneyMovedView.FLOW -> CashFlowWaterfallChart(
                    openingCash = summary.openingCashBalance,
                    collection = summary.totalCollection,
                    purchase = summary.totalPurchases,
                    expense = summary.totalExpenses,
                    withdrawal = summary.withdrawal,
                    closingCash = summary.cashBalance
                )
                MoneyMovedView.SPLIT -> MoneyAllocationBar(
                    collection = summary.totalCollection,
                    purchase = summary.totalPurchases,
                    expense = summary.totalExpenses,
                    withdrawal = summary.withdrawal,
                    provision = summary.provision,
                    outstandingPayments = summary.outstandingPayments
                )
                MoneyMovedView.FIGURES -> AccountBreakdown(summary)
            }
        }
    }
}

/**
 * Needs 2+ cached months to draw a trend. Below that, stays on screen with an explanation
 * instead of a silent early return — a shop with one month of data shouldn't look like a
 * shop with a broken screen (D10).
 */
@Composable
fun ProfitTrendCard(history: List<AccountSummary>, shopName: String, onShare: (() -> Unit)? = null) {
    DetailSectionCard(
        title = "Profit Trend",
        subtitle = shopName,
        caption = history.maxOfOrNull { it.lastUpdated }?.formatLastUpdated() ?: "",
        onShare = onShare
    ) {
        if (history.size < 2) {
            Text(
                text = "The trend line appears once this shop has two months of data. " +
                    "${history.size} so far.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        } else {
            AccountTrendChart(history = history.reversed())
        }
    }
}
