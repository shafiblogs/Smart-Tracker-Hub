package com.marsa.smarttrackerhub.ui.screens.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marsa.smarttracker.ui.theme.semanticStatusColors
import com.marsa.smarttrackerhub.domain.AccountSummary
import com.marsa.smarttrackerhub.ui.components.InfoRow
import com.marsa.smarttrackerhub.ui.screens.sale.BalanceComparisonRow
import com.marsa.smarttrackerhub.utils.formatMoney


/**
 * Created by Muhammed Shafi on 15/02/2026 — redesigned 25/07/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun AccountSummaryContent(summary: AccountSummary) {
    val colors = MaterialTheme.colorScheme
    val status = semanticStatusColors()

    Column {
        // ── Headline tiles ────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccountTile("Cash Balance", formatMoney(summary.cashBalance, 0), colors.onSurface, Modifier.weight(1f))
            ProfitTile("Gross Profit", summary.grossProfit, summary.grossMargin, status.success, status.danger, Modifier.weight(1f))
            ProfitTile("Net Profit", summary.netProfit, summary.netProfitMargin, status.success, status.danger, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(10.dp))

        // ── Balances (opening → closing) ──────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Balances", style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("Opening", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Text("Closing", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant,
                textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(6.dp))
        BalanceComparisonRow("Cash", summary.openingCashBalance, summary.cashBalance)
        BalanceComparisonRow("Outstanding", summary.openingOutstandingBalance, summary.outstandingBalance)

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(4.dp))

        // ── Details (clean labels) ────────────────────────────────────────
        InfoRow("Total Sale", summary.totalCollection, color = colors.primary)
        InfoRow("Total Purchase", summary.totalPurchases, color = colors.error)
        InfoRow("Total Expense", summary.totalExpenses, color = colors.error)
        InfoRow("Outstanding Payment", summary.outstandingPayments, color = colors.onSurface)
    }
}

@Composable
private fun AccountTile(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = valueColor)
    }
}

@Composable
private fun ProfitTile(
    label: String,
    amount: Double,
    marginPercent: Double,
    positiveColor: Color,
    negativeColor: Color,
    modifier: Modifier = Modifier
) {
    val color = if (amount >= 0) positiveColor else negativeColor
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(formatMoney(amount, 0), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = color)
        Text("${"%.0f".format(marginPercent)}% margin", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
