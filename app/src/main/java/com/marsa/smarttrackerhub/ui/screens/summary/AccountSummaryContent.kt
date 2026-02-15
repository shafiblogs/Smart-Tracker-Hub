package com.marsa.smarttrackerhub.ui.screens.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marsa.smarttrackerhub.domain.AccountSummary
import com.marsa.smarttrackerhub.ui.components.InfoRow
import com.marsa.smarttrackerhub.ui.screens.sale.BalanceComparisonRow


/**
 * Created by Muhammed Shafi on 15/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun AccountSummaryContent(summary: AccountSummary) {
    Column {
        // Balances Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Balances",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Opening",
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Closing",
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }

        Divider(Modifier.padding(vertical = 6.dp))

        BalanceComparisonRow("Cash", summary.openingCashBalance, summary.cashBalance)
        BalanceComparisonRow(
            "Outstanding",
            summary.openingOutstandingBalance,
            summary.outstandingBalance
        )

        Divider(Modifier.padding(vertical = 8.dp))

        // Totals Section
        InfoRow(
            "💰 Profit Margin",
            summary.netProfitMargin,
            color = MaterialTheme.colorScheme.primary
        )
        InfoRow("💳 Sales Margin", summary.grossMargin, color = MaterialTheme.colorScheme.primary)
        InfoRow("💰 Net Profit", summary.netProfit)
        InfoRow("🛒 Gross Profit", summary.grossProfit)
        InfoRow("💰 Total Sale", summary.totalCollection)
        InfoRow("💳 Total Expense", summary.totalExpenses)
        InfoRow("🛒 Total Purchase", summary.totalPurchases)
        InfoRow("💳 Outstanding Payment", summary.outstandingPayments)
    }
}