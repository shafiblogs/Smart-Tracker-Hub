package com.marsa.smarttrackerhub.ui.screens.purchase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.buildAnnotatedString
import com.marsa.smarttrackerhub.ui.components.AedText
import com.marsa.smarttrackerhub.ui.components.appendAed
import com.marsa.smarttrackerhub.ui.screens.chart.PurchaseCategoryChartData
import com.marsa.smarttrackerhub.ui.screens.chart.PurchaseChartStatistics
import com.marsa.smarttrackerhub.ui.screens.chart.purchaseAchievementColor
import com.marsa.smarttrackerhub.ui.screens.chart.purchaseVarianceColor
import com.marsa.smarttracker.ui.theme.semanticStatusColors
import kotlin.math.roundToInt

/**
 * Category-wise purchase breakdown as an actual-vs-target comparison (same model as the home
 * screen): headline tiles (Spent, Budget, Variance), an overall progress bar, then one row per
 * category with `actual of target`, a progress bar (fraction = actual/target) and the signed
 * variance. Per-category bar colour follows `purchaseAchievementColor` (green 90–120% / amber
 * 75–90% / red below 75% or above 120% — a large overrun is a problem, not an achievement).
 * Target = previous month's category amount × 1.10 (floored); primary/neutral when no target.
 */
@Composable
fun PurchaseBreakdownSection(
    categories: List<PurchaseCategoryChartData>,
    statistics: PurchaseChartStatistics?
) {
    val colors = MaterialTheme.colorScheme
    val status = semanticStatusColors()
    if (categories.isEmpty()) {
        Text(
            text = "No purchase data for this month",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        return
    }

    val totalActual = statistics?.totalActual ?: categories.sumOf { it.actual }
    val totalTarget = statistics?.totalTarget ?: 0.0
    val hasBudget = totalTarget > 0.0
    val overallPct = statistics?.achievementPercentage ?: 0.0
    // Purchase's bar means the opposite of Sales' — a fuller bar is MORE spend, i.e. worse —
    // so state it as budget variance rather than achievement (matches the Home card).
    val overallVariancePct = overallPct - 100.0
    val overallColor = purchaseVarianceColor(overallPct, hasBudget, status) ?: colors.onSurface
    val overallCaption = when {
        !hasBudget -> null
        kotlin.math.abs(overallVariancePct) <= 2.0 -> "on budget"
        overallVariancePct > 0 -> "over budget"
        else -> "under budget"
    }

    Column {
        // ── Headline tiles ────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PurchaseTile("Spent", totalActual, colors.onSurface, Modifier.weight(1f))
            PurchaseTile("Budget", totalTarget, colors.onSurface, Modifier.weight(1f))
            VarianceTile(
                variancePercent = if (hasBudget) overallVariancePct else null,
                caption = overallCaption,
                color = overallColor,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Overall vs-budget progress ────────────────────────────────────
        if (hasBudget) {
            Spacer(modifier = Modifier.height(10.dp))
            ProgressBar((totalActual / totalTarget).toFloat(), overallColor, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(6.dp))

        // ── Per-category actual vs target ─────────────────────────────────
        categories.forEach { c ->
            val barColor = if (!c.hasTarget) colors.primary
            else purchaseAchievementColor(c.achievementPercentage, status)
            val fraction = if (c.hasTarget && c.target > 0) (c.actual / c.target).toFloat() else 1f

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = c.categoryName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = buildAnnotatedString {
                            appendAed(c.actual)
                            append(" of ")
                            appendAed(c.target)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressBar(fraction, barColor, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (c.hasTarget) {
                            val v = c.achievementPercentage - 100.0
                            "${if (v > 0) "+" else ""}${v.roundToInt()}%"
                        } else "—",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = barColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(6.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun PurchaseTile(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    PurchaseTileChrome(label, modifier) {
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = valueColor)
    }
}

/** Money variant — renders the AED glyph via [AedText] instead of a plain formatted string. */
@Composable
private fun PurchaseTile(label: String, amount: Double, valueColor: Color, modifier: Modifier = Modifier) {
    PurchaseTileChrome(label, modifier) {
        AedText(
            amount = amount,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor
        )
    }
}

/** Third headline tile — signed budget variance ("+12%"/"−8%") with an "over/under/on budget" caption. */
@Composable
private fun VarianceTile(variancePercent: Double?, caption: String?, color: Color, modifier: Modifier = Modifier) {
    PurchaseTileChrome("Variance", modifier) {
        Text(
            text = if (variancePercent != null) "${if (variancePercent > 0) "+" else ""}${variancePercent.roundToInt()}%" else "—",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
        if (caption != null) {
            Text(caption, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
private fun PurchaseTileChrome(label: String, modifier: Modifier, value: @Composable () -> Unit) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        value()
    }
}
