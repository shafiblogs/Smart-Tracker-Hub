package com.marsa.smarttrackerhub.ui.screens.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Summary card for purchase data — mirrors the sale [StatisticsCard] structure.
 *
 * Shows: shop name + month label + achievement dot (via [ChartTitle]),
 * then two stat rows: Total Budget / Total Actual and Achievement / On Target.
 */
@Composable
fun PurchaseStatisticsCard(
    statistics: PurchaseChartStatistics,
    shopAddress: String,
    periodLabel: String = "",
    modifier: Modifier = Modifier,
    onShareClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    fun achievementColor(pct: Double): Color = when {
        pct >= 100 -> Color(0xFF22C55E) // SuccessGreen
        pct >= 85  -> Color(0xFFF59E0B) // WarningAmber
        else       -> colors.error
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            Column {
                // ── Header row: ChartTitle + share button ─────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartTitle(
                        shopAddress = shopAddress,
                        periodLabel = periodLabel,
                        achievementPercentage = if (statistics.totalTarget > 0)
                            statistics.achievementPercentage else 100.0,
                        modifier = Modifier
                            .weight(1f)
                            .padding(0.dp)
                    )

                    onShareClick?.let { callback ->
                        IconButton(
                            onClick = callback,
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Purchase Statistics",
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Row 1: Total Budget | Total Actual ────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PurchaseStatItem(
                        label = "Total Budget",
                        value = String.format("%.0f", statistics.totalTarget),
                        valueColor = colors.primary
                    )
                    PurchaseStatItem(
                        label = "Total Actual",
                        value = String.format("%.0f", statistics.totalActual),
                        valueColor = achievementColor(statistics.achievementPercentage)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Row 2: Achievement | On Target ────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val hasTarget = statistics.totalTarget > 0
                    PurchaseStatItem(
                        label = "Achievement",
                        value = if (hasTarget)
                            String.format("%.0f%%", statistics.achievementPercentage)
                        else "—",
                        valueColor = if (hasTarget)
                            achievementColor(statistics.achievementPercentage)
                        else colors.onSurfaceVariant
                    )

                    val diff = statistics.totalActual - statistics.totalTarget
                    val icon = if (diff >= 0) "↑" else "↓"
                    PurchaseStatItem(
                        label = "On Target",
                        value = "${statistics.categoriesOnTarget} / ${statistics.totalCategories}",
                        valueColor = if (statistics.categoriesOnTarget == statistics.totalCategories)
                            Color(0xFF22C55E)
                        else
                            colors.error
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchaseStatItem(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
