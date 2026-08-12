package com.marsa.smarttrackerhub.ui.screens.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marsa.smarttracker.ui.theme.semanticStatusColors
import com.marsa.smarttrackerhub.domain.AccountSummary
import com.marsa.smarttrackerhub.ui.components.AedText
import com.marsa.smarttrackerhub.ui.screens.sale.BalanceComparisonRow


/**
 * Created by Muhammed Shafi on 15/02/2026 — redesigned 25/07/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

/** Headline tiles: Cash Balance + Gross/Net Profit (sign-coloured). */
@Composable
fun AccountProfitTiles(summary: AccountSummary) {
    val colors = MaterialTheme.colorScheme
    val status = semanticStatusColors()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AccountTile("Balance", summary.cashBalance, colors.onSurface, Modifier.weight(1f))
        ProfitTile("Gross Profit", summary.grossProfit, summary.grossMargin, status.success, status.danger, Modifier.weight(1f))
        ProfitTile("Net Profit", summary.netProfit, summary.netProfitMargin, status.success, status.danger, Modifier.weight(1f))
    }
}

/** Opening→closing balances + the collection/purchase/expense/outstanding breakdown. */
@Composable
fun AccountBreakdown(summary: AccountSummary) {
    val colors = MaterialTheme.colorScheme
    Column {
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
        BalanceComparisonRow("Account", summary.openingAccountBalance, summary.accountBalance)
        BalanceComparisonRow("Outstanding", summary.openingOutstandingBalance, summary.outstandingBalance)

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(4.dp))

        AccountAmountRow("Collection", summary.totalCollection, colors.primary)
        AccountAmountRow("Purchase", summary.totalPurchases, colors.error)
        AccountAmountRow("Expense", summary.totalExpenses, colors.error)
        AccountAmountRow("Provision", summary.provision, colors.secondary)
        AccountAmountRow("Withdrawal", summary.withdrawal, colors.error)
        AccountAmountRow("Outstanding Payment", summary.outstandingPayments, colors.onSurface)

        val cashOut = summary.openingCashBalance + summary.totalCollection - summary.cashBalance
        AccountAmountRow("Cash Out", cashOut, colors.error)

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(4.dp))

        val status = semanticStatusColors()
        AccountAmountRow(
            "Gross Profit", summary.grossProfit,
            if (summary.grossProfit >= 0) status.success else status.danger,
            trailingLabel = "${"%.0f".format(summary.grossMargin)}%"
        )
        AccountAmountRow(
            "Net Profit", summary.netProfit,
            if (summary.netProfit >= 0) status.success else status.danger,
            trailingLabel = "${"%.0f".format(summary.netProfitMargin)}%"
        )
    }
}

/** Label (left) + amount (right, optionally with a trailing % label) — always visible (unlike InfoRow which hides zeros). */
@Composable
private fun AccountAmountRow(label: String, amount: Double, valueColor: Color, trailingLabel: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            AedText(
                amount = amount,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = valueColor
            )
            if (trailingLabel != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    trailingLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Full content (tiles + breakdown) — kept for any single-card use. */
@Composable
fun AccountSummaryContent(summary: AccountSummary) {
    val colors = MaterialTheme.colorScheme
    Column {
        AccountProfitTiles(summary)
        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(10.dp))
        AccountBreakdown(summary)
    }
}

@Composable
private fun AccountTile(label: String, amount: Double, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        AedText(amount = amount, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = valueColor)
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
        AedText(amount = amount, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = color)
        Text("${"%.0f".format(marginPercent)}% margin", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
