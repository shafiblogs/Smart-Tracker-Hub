package com.marsa.smarttrackerhub.ui.screens.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.sp
import com.marsa.smarttrackerhub.domain.MonthlySummary
import com.marsa.smarttrackerhub.ui.components.AchievementBar
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
        // Unclamped, with a fixed target tick, so beating target (>100%) is visibly different
        // from landing exactly on it — matches the Home card's AchievementBar. The tick's own
        // "target" label states the target positionally, so no separate AED line is needed.
        if (hasTarget) {
            Spacer(modifier = Modifier.height(10.dp))
            AchievementBar(actual = avg, target = target, color = achColor, label = "target")
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(10.dp))

        // ── Balances (opening → closing → change) ──────────────────────────
        BalanceHeaderRow()
        Spacer(modifier = Modifier.height(6.dp))
        BalanceComparisonRow("Cash", summary.openingCashBalance, summary.cashBalance)
        BalanceComparisonRow("Account", summary.openingAccountBalance, summary.accountBalance)
        BalanceComparisonRow("Credit", summary.openingCreditBalance, summary.creditSaleBalance)

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(4.dp))

        // ── Details, grouped ───────────────────────────────────────────────
        // Neutral facts carry no judgement — colour reserved for Gross/Net Profit,
        // where a negative value is genuinely bad news. See SemanticStatusColors.kt.
        // showZero defaults to true so the row set is fixed month to month; a zero row
        // ("no credit sale this month") is real information, not something to hide.
        GroupHeading("Purchase")
        InfoRow("Total", summary.totalPurchases, color = colors.onSurface)
        InfoRow("Credit", summary.creditPurchase, color = colors.onSurface)
        InfoRow("VAT", summary.vatPurchase, color = colors.onSurface)

        Spacer(modifier = Modifier.height(6.dp))
        GroupHeading("Cash movement")
        InfoRow("In", summary.totalCashIn, color = colors.onSurface)
        InfoRow("Out", summary.totalCashOut, color = colors.onSurface)
        InfoRow("Expense", summary.totalExpenses, color = colors.onSurface)

        Spacer(modifier = Modifier.height(6.dp))
        GroupHeading("Credit sale")
        InfoRow("Raised", summary.totalCreditSale, color = colors.onSurface)
        InfoRow("Payment received", summary.creditSalePayment, color = colors.onSurface)
    }
}

@Composable
private fun GroupHeading(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 2.dp)
    )
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

/** Header row for [BalanceComparisonRow] — column weights must match. */
@Composable
fun BalanceHeaderRow() {
    val colors = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("Balances", style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant, modifier = Modifier.weight(1.3f))
        Text("Opening", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant,
            textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text("Closing", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant,
            textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text("Change", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant,
            textAlign = TextAlign.End, modifier = Modifier.weight(0.85f))
    }
}

/**
 * Label / Opening / Closing / Change — weighted 1.3/1/1/0.85 so the label reads at full width
 * and each figure column gets only what a number needs, not a full 1/3 share.
 */
@Composable
fun BalanceComparisonRow(label: String, opening: Double, current: Double) {
    val status = semanticStatusColors()
    val change = current - opening
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1.3f)
        )
        AedText(
            amount = opening,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        AedText(
            amount = current,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = if (current < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        AedText(
            amount = change,
            prefix = if (change >= 0) "+" else "",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (change >= 0) status.success else status.danger,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.85f)
        )
    }
}
