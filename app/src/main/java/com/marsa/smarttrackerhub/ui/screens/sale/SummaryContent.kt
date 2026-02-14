package com.marsa.smarttrackerhub.ui.screens.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marsa.smarttrackerhub.domain.MonthlySummary
import com.marsa.smarttrackerhub.ui.components.InfoRow


/**
 * Created by Muhammed Shafi on 14/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun SummaryContent(summary: MonthlySummary) {
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
        BalanceComparisonRow("Account", summary.openingAccountBalance, summary.accountBalance)
        BalanceComparisonRow("Credit", summary.openingCreditBalance, summary.creditSaleBalance)

        Divider(Modifier.padding(vertical = 8.dp))

        // Totals Section
        InfoRow(
            "💰 Average Sale",
            summary.averageSale ?: 0.0,
            color = MaterialTheme.colorScheme.primary
        )
        InfoRow("💰 Total Sale", summary.totalSales, color = MaterialTheme.colorScheme.primary)
        InfoRow("🛒 Total Purchase", summary.totalPurchases, color = MaterialTheme.colorScheme.error)
        InfoRow("💳 Total Expense", summary.totalExpenses, color = MaterialTheme.colorScheme.error)
        InfoRow("💰 Total Cash In", summary.totalCashIn, color = MaterialTheme.colorScheme.primary)
        InfoRow("🛒 Total Cash Out", summary.totalCashOut, color = MaterialTheme.colorScheme.error)
        InfoRow(
            "💳 Credit Sale Total",
            summary.totalCreditSale,
            color = MaterialTheme.colorScheme.error
        )
        InfoRow(
            "💰 Credit Sale Payment",
            summary.creditSalePayment,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun BalanceComparisonRow(label: String, opening: Double, current: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "₹%.2f".format(opening),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "₹%.2f".format(current),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = if (current < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}