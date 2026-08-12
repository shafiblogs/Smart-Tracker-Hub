package com.marsa.smarttrackerhub.ui.screens.sale

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.marsa.smarttrackerhub.domain.MonthlySummary
import com.marsa.smarttrackerhub.ui.components.AedText
import com.marsa.smarttrackerhub.ui.components.InfoRow
import com.marsa.smarttrackerhub.ui.screens.chart.salesAchievementColor
import com.marsa.smarttracker.ui.theme.semanticStatusColors


/**
 * Created by Muhammed Shafi on 14/02/2026 — redesigned 25/07/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun SummaryContent(summary: MonthlySummary) {
    val colors = MaterialTheme.colorScheme
    val status = semanticStatusColors()
    val avg = summary.averageSale ?: 0.0
    val target = summary.targetSale
    val hasTarget = target > 0.0
    val achievementPct = if (hasTarget) avg / target * 100 else 0.0
    val achColor = if (hasTarget) salesAchievementColor(achievementPct, status)
    else colors.onSurfaceVariant

    Column {
        // ── Headline tiles ────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatTile("Total Sale", summary.totalSales, colors.onSurface, Modifier.weight(1f))
            StatTile("Avg Sale", avg, colors.onSurface, Modifier.weight(1f))
            StatTile(
                "Achievement",
                if (hasTarget) "${"%.0f".format(achievementPct)}%" else "—",
                achColor,
                Modifier.weight(1f)
            )
        }

        // ── Sales-vs-target progress ──────────────────────────────────────
        if (hasTarget) {
            Spacer(modifier = Modifier.height(10.dp))
            val fraction = (avg / target).toFloat().coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(colors.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .background(achColor, RoundedCornerShape(3.dp))
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            AedText(
                amount = target,
                prefix = "Target ",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
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
        BalanceComparisonRow("Account", summary.openingAccountBalance, summary.accountBalance)
        BalanceComparisonRow("Credit", summary.openingCreditBalance, summary.creditSaleBalance)

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(4.dp))

        // ── Details (clean labels) ────────────────────────────────────────
        InfoRow("Total Purchase", summary.totalPurchases, color = colors.error)
        InfoRow("Credit Purchase", summary.creditPurchase, color = colors.error)
        InfoRow("VAT Purchase", summary.vatPurchase, color = colors.onSurface)
        InfoRow("Total Expense", summary.totalExpenses, color = colors.error)
        InfoRow("Total Cash In", summary.totalCashIn, color = colors.primary)
        InfoRow("Total Cash Out", summary.totalCashOut, color = colors.error)
        InfoRow("Credit Sale", summary.totalCreditSale, color = colors.onSurface)
        InfoRow("Credit Sale Payment", summary.creditSalePayment, color = colors.primary)
    }
}

@Composable
private fun StatTile(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    StatTileChrome(label, modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor
        )
    }
}

/** Money variant — renders the AED glyph via [AedText] instead of a plain formatted string. */
@Composable
private fun StatTile(label: String, amount: Double, valueColor: Color, modifier: Modifier = Modifier) {
    StatTileChrome(label, modifier) {
        AedText(
            amount = amount,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor
        )
    }
}

@Composable
private fun StatTileChrome(label: String, modifier: Modifier, value: @Composable () -> Unit) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        value()
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
        AedText(
            amount = opening,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        AedText(
            amount = current,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = if (current < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}
