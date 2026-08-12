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
import androidx.compose.ui.unit.dp
import com.marsa.smarttracker.ui.theme.semanticStatusColors
import com.marsa.smarttrackerhub.domain.AccountSummary
import com.marsa.smarttrackerhub.ui.components.AedText
import com.marsa.smarttrackerhub.ui.screens.sale.BalanceComparisonRow
import com.marsa.smarttrackerhub.ui.screens.sale.BalanceHeaderRow


/**
 * Created by Muhammed Shafi on 15/02/2026 — redesigned 25/07/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

/**
 * Headline tiles: Cash Balance + Gross/Net Profit (sign-coloured). The Balance tile carries
 * its own opening-to-closing movement as a caption — the only one of the three that didn't
 * show its month's movement anywhere else (D8).
 */
@Composable
fun AccountProfitTiles(summary: AccountSummary) {
    val colors = MaterialTheme.colorScheme
    val status = semanticStatusColors()
    val balanceChange = summary.cashBalance - summary.openingCashBalance
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AccountTile(
            "Balance", summary.cashBalance, colors.onSurface, Modifier.weight(1f),
            captionAmount = balanceChange,
            captionColor = if (balanceChange >= 0) status.success else status.danger
        )
        ProfitTile("Gross Profit", summary.grossProfit, summary.grossMargin, status.success, status.danger, Modifier.weight(1f))
        ProfitTile("Net Profit", summary.netProfit, summary.netProfitMargin, status.success, status.danger, Modifier.weight(1f))
    }
}

/** Opening→closing balances + the collection/purchase/expense/outstanding breakdown. */
@Composable
fun AccountBreakdown(summary: AccountSummary) {
    val colors = MaterialTheme.colorScheme
    Column {
        BalanceHeaderRow()
        Spacer(modifier = Modifier.height(6.dp))
        BalanceComparisonRow("Cash", summary.openingCashBalance, summary.cashBalance)
        BalanceComparisonRow("Account", summary.openingAccountBalance, summary.accountBalance)
        BalanceComparisonRow("Outstanding", summary.openingOutstandingBalance, summary.outstandingBalance)

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(4.dp))

        // Neutral facts carry no judgement — colour reserved for Gross/Net Profit,
        // where a negative value is genuinely bad news. See SemanticStatusColors.kt.
        AccountAmountRow("Collection", summary.totalCollection, colors.onSurface)
        AccountAmountRow("Purchase", summary.totalPurchases, colors.onSurface)
        AccountAmountRow("Expense", summary.totalExpenses, colors.onSurface)
        AccountAmountRow("Provision", summary.provision, colors.onSurface)
        AccountAmountRow("Withdrawal", summary.withdrawal, colors.onSurface)
        AccountAmountRow("Outstanding Payment", summary.outstandingPayments, colors.onSurface)

        val cashOut = summary.openingCashBalance + summary.totalCollection - summary.cashBalance
        AccountAmountRow("Cash Out", cashOut, colors.onSurface)

        // Gross/Net Profit stop here — AccountProfitTiles already states both figures with
        // their margins at the top of the screen; repeating them a card lower said nothing new.
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
private fun AccountTile(
    label: String,
    amount: Double,
    valueColor: Color,
    modifier: Modifier = Modifier,
    captionAmount: Double? = null,
    captionColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        AedText(amount = amount, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = valueColor)
        if (captionAmount != null) {
            AedText(
                amount = captionAmount,
                prefix = if (captionAmount >= 0) "+" else "",
                suffix = " this month",
                style = MaterialTheme.typography.labelSmall,
                color = captionColor
            )
        }
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
