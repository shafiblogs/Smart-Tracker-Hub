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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marsa.smarttrackerhub.domain.ChartStatistics
import com.marsa.smarttrackerhub.utils.formatMoney
import com.marsa.smarttracker.ui.theme.semanticStatusColors

/**
 * Shop statistics card — stacked vertical layout with Sales above Purchase.
 *
 * Each section shows:
 *  - Achievement % headline
 *  - Actual vs Target line (formatted currency)
 *  - Achievement bar (0–100%)
 *  - Supporting metric (margin % for Sales, on-target count for Purchase)
 */
@Composable
fun UnifiedStatisticsCard(
    salesStatistics: ChartStatistics,
    purchaseStatistics: PurchaseChartStatistics,
    shopName: String,
    periodLabel: String,
    salesMargin: Double = 0.0,
    caption: String? = null,
    modifier: Modifier = Modifier,
    onShareClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val status = semanticStatusColors()

    fun getAchievementColor(percentage: Double): Color = salesAchievementColor(percentage, status)

    fun getPurchaseAchievementColor(percentage: Double): Color = purchaseAchievementColor(percentage, status)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // ── Header: Shop + Period + Share Button ──────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shopName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    if (!caption.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = caption,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                onShareClick?.let { callback ->
                    IconButton(
                        onClick = callback,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Statistics",
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = colors.outlineVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Stacked Sections ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // SALES SECTION
                SalesMetricsSection(
                    salesStatistics = salesStatistics,
                    salesMargin = salesMargin,
                    getAchievementColor = ::getAchievementColor,
                    status = status
                )

                HorizontalDivider(color = colors.outlineVariant)

                // PURCHASE SECTION
                PurchaseMetricsSection(
                    purchaseStatistics = purchaseStatistics,
                    getPurchaseAchievementColor = ::getPurchaseAchievementColor,
                    status = status
                )
            }
        }
    }
}

@Composable
private fun SalesMetricsSection(
    salesStatistics: ChartStatistics,
    salesMargin: Double,
    getAchievementColor: (Double) -> Color,
    status: com.marsa.smarttracker.ui.theme.SemanticStatusColors
) {
    val colors = MaterialTheme.colorScheme
    val targetAvg = salesStatistics.totalTarget / salesStatistics.totalMonths
    val actualAvg = salesStatistics.totalAverage / salesStatistics.totalMonths
    val achievement = salesStatistics.averageAchievementPercentage
    val achievementColor = getAchievementColor(achievement)

    Column {
        // Section header: "SALES" label on the left, achievement % right-aligned on the same row.
        SectionHeaderRow(label = "SALES") {
            Text(
                text = "${"%.0f".format(achievement)}%",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = achievementColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Target vs Actual: single line showing actual of target (e.g., "Đ540,000 of Đ500,000 target")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatMoney(actualAvg, 0),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = achievementColor
            )
            Text(
                text = "of ${formatMoney(targetAvg, 0)} target",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Achievement bar with target tick
        AchievementBar(
            actual = actualAvg,
            target = targetAvg,
            color = achievementColor,
            label = "target"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Margin stat
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Margin",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Text(
                text = "${"%.0f".format(salesMargin)}%",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = salesMarginColor(salesMargin, status)
            )
        }
    }
}

@Composable
private fun PurchaseMetricsSection(
    purchaseStatistics: PurchaseChartStatistics,
    getPurchaseAchievementColor: (Double) -> Color,
    status: com.marsa.smarttracker.ui.theme.SemanticStatusColors
) {
    val colors = MaterialTheme.colorScheme
    val achievement = purchaseStatistics.achievementPercentage
    val achievementColor = if (purchaseStatistics.totalTarget > 0) getPurchaseAchievementColor(achievement) else colors.onSurfaceVariant

    Column {
        // Section header: "PURCHASE" label on the left, achievement % (or "—" when there's no
        // budget) right-aligned on the same row.
        SectionHeaderRow(label = "PURCHASE") {
            Text(
                text = if (purchaseStatistics.totalTarget > 0) "${"%.0f".format(achievement)}%" else "—",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = achievementColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Budget vs Actual: single line showing actual of budget (e.g., "Đ368,000 of Đ400,000 budget")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatMoney(purchaseStatistics.totalActual, 0),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = achievementColor
            )
            Text(
                text = "of ${formatMoney(purchaseStatistics.totalTarget, 0)} budget",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Achievement bar with budget tick
        if (purchaseStatistics.totalTarget > 0) {
            AchievementBar(
                actual = purchaseStatistics.totalActual,
                target = purchaseStatistics.totalTarget,
                color = achievementColor,
                label = "budget"
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.outlineVariant.copy(alpha = 0.25f))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // On-target stat
        val onTargetPercentage = if (purchaseStatistics.totalCategories > 0)
            (purchaseStatistics.categoriesOnTarget.toDouble() / purchaseStatistics.totalCategories) * 100
        else 0.0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "On Target",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Text(
                text = "${purchaseStatistics.categoriesOnTarget} / ${purchaseStatistics.totalCategories}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = when {
                    onTargetPercentage >= 80 -> status.success
                    onTargetPercentage >= 50 -> status.warning
                    else -> status.danger
                }
            )
        }
    }
}

/** Section header: bold label on the left, arbitrary trailing content (the achievement %)
 *  right-aligned on the same row. */
@Composable
private fun SectionHeaderRow(
    label: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        trailing()
    }
}

@Composable
private fun AchievementBar(
    actual: Double,
    target: Double,
    color: Color,
    label: String = "target",
    modifier: Modifier = Modifier
) {
    val maxValue = kotlin.math.max(actual, target).coerceAtLeast(1.0)
    val filledFraction = (actual / maxValue).coerceIn(0.0, 1.0).toFloat()
    val targetFraction = (target / maxValue).coerceIn(0.0, 1.0).toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            // Filled portion
            if (filledFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(filledFraction)
                        .fillMaxHeight()
                        .background(color)
                )
            }
            // Target tick mark
            Box(
                modifier = Modifier
                    .fillMaxWidth(targetFraction)
                    .fillMaxHeight()
                    .padding(end = 1.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(targetFraction.coerceIn(0f, 0.95f)))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight((1f - targetFraction).coerceIn(0.05f, 1f)))
        }
    }
}
