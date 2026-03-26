package com.marsa.smarttrackerhub.ui.screens.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Horizontal-bar breakdown chart for purchase categories.
 *
 * Each row shows:
 *   [Category name] [████░░░░] [actual / target]
 *
 * Colour rules (purchases — higher is better, more purchase → more sale):
 *   ≥ 100 % of target  → SuccessGreen  (hit or exceeded target)
 *   85–100 %           → WarningAmber  (close but under)
 *   < 85 %             → ErrorRed      (significantly under target)
 *
 * Created by Muhammed Shafi on 24/03/2026.
 * Moro Hub
 */
@Composable
fun PurchaseCategoryChart(
    categories: List<PurchaseCategoryChartData>,
    statistics: PurchaseChartStatistics,
    shopAddress: String = "",
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Column(modifier = modifier) {

        // ── Header: shop name + period — matches sales card style ────────────
        ChartTitle(
            shopAddress = shopAddress,
            periodLabel = statistics.monthLabel,
            // Pass 100.0 when there is no previous-month target so the indicator
            // dot stays green (neutral — nothing to compare against yet)
            achievementPercentage = if (statistics.totalTarget > 0)
                statistics.achievementPercentage else 100.0
        )

        // ── Sub-header + rest of content ────────────────────────────────────
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {

        // Mirror individual row logic: no target data → use primary (neutral), not error red
        val totalColor = if (statistics.totalTarget > 0)
            purchaseColor(statistics.achievementPercentage, colors.error)
        else
            colors.primary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${statistics.categoriesUnderTarget} / ${statistics.totalCategories} on target",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            if (statistics.totalTarget > 0) {
                Text(
                    text = "${statistics.achievementPercentage.toInt()}% of budget",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = totalColor
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = colors.outlineVariant)
        Spacer(modifier = Modifier.height(8.dp))

        // ── Column headers ───────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.width(120.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Progress",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Actual / Target",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(110.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Category rows ────────────────────────────────────────────────────
        categories.forEach { category ->
            PurchaseCategoryRow(category = category)
            Spacer(modifier = Modifier.height(12.dp))
        }

        HorizontalDivider(color = colors.outlineVariant)
        Spacer(modifier = Modifier.height(10.dp))

        // ── Total row ────────────────────────────────────────────────────────
        val totalFraction = if (statistics.totalTarget > 0)
            minOf(1f, (statistics.totalActual / statistics.totalTarget).toFloat())
        else 1f

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TOTAL",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                modifier = Modifier.width(120.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(colors.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(totalFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(7.dp))
                        .background(totalColor)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(110.dp)
            ) {
                Text(
                    text = formatPurchaseAmount(statistics.totalActual),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = totalColor,
                    fontSize = 11.sp
                )
                if (statistics.totalTarget > 0) {
                    Text(
                        text = formatPurchaseAmount(statistics.totalTarget),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }

        } // end inner Column
    }
}

// ── Private helpers ──────────────────────────────────────────────────────────

@Composable
private fun PurchaseCategoryRow(category: PurchaseCategoryChartData) {
    val colors = MaterialTheme.colorScheme
    // No previous month → render in primary (neutral, no baseline to compare)
    // Has target → colour by achievement: ≥100% green, 85-100% amber, <85% red
    val barColor = if (!category.hasTarget) colors.primary
                   else purchaseColor(category.achievementPercentage, colors.error)
    val fraction = when {
        !category.hasTarget -> 1f
        category.target > 0 -> minOf(1f, (category.actual / category.target).toFloat())
        else -> 0f
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category.categoryName,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(120.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(barColor)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(110.dp)
        ) {
            Text(
                text = formatPurchaseAmount(category.actual),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = barColor,
                fontSize = 11.sp
            )
            if (category.hasTarget) {
                Text(
                    text = formatPurchaseAmount(category.target),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Returns a purchase-appropriate colour.
 * More purchase = more sale, so exceeding the target is good:
 *   ≥ 100% → SuccessGreen  (on or above target)
 *   85–100% → WarningAmber (close but under)
 *   < 85%  → ErrorRed      (significantly under)
 */
private fun purchaseColor(percentage: Double, errorColor: Color): Color = when {
    percentage >= 100.0 -> Color(0xFF22C55E)
    percentage >= 85.0  -> Color(0xFFF59E0B)
    else                -> errorColor
}

private fun formatPurchaseAmount(amount: Double): String = when {
    amount >= 1_000_000 -> String.format("%.1fM", amount / 1_000_000)
    amount >= 1_000     -> String.format("%.1fK", amount / 1_000)
    else                -> String.format("%.0f", amount)
}
