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

    fun getAchievementColor(percentage: Double): Color = salesAchievementColor(percentage, colors.error)

    fun getPurchaseAchievementColor(percentage: Double): Color = purchaseAchievementColor(percentage, colors.error)

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
                    getAchievementColor = ::getAchievementColor
                )

                HorizontalDivider(color = colors.outlineVariant)

                // PURCHASE SECTION
                PurchaseMetricsSection(
                    purchaseStatistics = purchaseStatistics,
                    getPurchaseAchievementColor = ::getPurchaseAchievementColor
                )
            }
        }
    }
}

@Composable
private fun SalesMetricsSection(
    salesStatistics: ChartStatistics,
    salesMargin: Double,
    getAchievementColor: (Double) -> Color
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

        // Achievement bar
        AchievementBar(percentage = achievement, color = achievementColor)

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
                color = when {
                    salesMargin >= 30 -> ChartSuccessGreen
                    salesMargin >= 10 -> ChartWarningAmber
                    else -> colors.error
                }
            )
        }
    }
}

@Composable
private fun PurchaseMetricsSection(
    purchaseStatistics: PurchaseChartStatistics,
    getPurchaseAchievementColor: (Double) -> Color
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

        // Achievement bar
        if (purchaseStatistics.totalTarget > 0) {
            AchievementBar(percentage = achievement, color = achievementColor)
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
                    onTargetPercentage >= 80 -> ChartSuccessGreen
                    onTargetPercentage >= 50 -> ChartWarningAmber
                    else -> colors.error
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
    percentage: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val filled = (percentage.coerceIn(0.0, 100.0) / 100.0).toFloat()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        if (filled > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(filled)
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}
