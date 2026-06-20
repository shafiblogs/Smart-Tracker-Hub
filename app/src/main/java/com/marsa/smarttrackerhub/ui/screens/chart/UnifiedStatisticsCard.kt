package com.marsa.smarttrackerhub.ui.screens.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsa.smarttrackerhub.domain.ChartStatistics

/**
 * Unified two-column statistics card showing both sales and purchase metrics side-by-side.
 *
 * Left column: Sales (Target, Actual, Achievement %, Margin)
 * Right column: Purchase (Budget, Actual, Achievement %, Categories On Target)
 */
@Composable
fun UnifiedStatisticsCard(
    salesStatistics: ChartStatistics,
    purchaseStatistics: PurchaseChartStatistics,
    shopName: String,
    periodLabel: String,
    salesMargin: Double = 0.0,
    modifier: Modifier = Modifier,
    onShareClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    fun getAchievementColor(percentage: Double): Color {
        return when {
            percentage >= 100 -> Color(0xFF22C55E) // SuccessGreen
            percentage >= 90  -> Color(0xFFF59E0B) // WarningAmber
            else              -> colors.error
        }
    }

    fun getPurchaseAchievementColor(percentage: Double): Color {
        return when {
            percentage >= 100 -> Color(0xFF22C55E) // SuccessGreen
            percentage >= 85  -> Color(0xFFF59E0B) // WarningAmber
            else              -> colors.error
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        color = colors.onSurface,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
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

            // ── Two-Column Layout ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // LEFT COLUMN: SALES
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    // SALES header with colored circle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = getAchievementColor(salesStatistics.averageAchievementPercentage),
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = "SALES",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Sales: Target & Actual
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        UnifiedStatItem(
                            label = "Target",
                            value = String.format(
                                "%.0f",
                                salesStatistics.totalTarget / salesStatistics.totalMonths
                            ),
                            valueColor = colors.primary,
                            modifier = Modifier.weight(1f)
                        )
                        UnifiedStatItem(
                            label = "Actual",
                            value = String.format(
                                "%.0f",
                                salesStatistics.totalAverage / salesStatistics.totalMonths
                            ),
                            valueColor = getAchievementColor(salesStatistics.averageAchievementPercentage),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Sales: Achievement & Margin
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        UnifiedStatItem(
                            label = "Met",
                            value = String.format("%.0f%%", salesStatistics.averageAchievementPercentage),
                            valueColor = getAchievementColor(salesStatistics.averageAchievementPercentage),
                            modifier = Modifier.weight(1f)
                        )

                        UnifiedStatItem(
                            label = "Margin",
                            value = String.format("%.0f%%", salesMargin),
                            valueColor = when {
                                salesMargin >= 30 -> Color(0xFF22C55E)    // SuccessGreen (good)
                                salesMargin >= 10 -> Color(0xFFF59E0B)    // WarningAmber (warning)
                                else              -> colors.error          // ErrorRed (poor)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // VERTICAL DIVIDER
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(120.dp)
                        .background(colors.outlineVariant)
                )

                // RIGHT COLUMN: PURCHASES
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    // PURCHASES header with colored circle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = getPurchaseAchievementColor(purchaseStatistics.achievementPercentage),
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = "PURCHASE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Purchase: Budget & Actual
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        UnifiedStatItem(
                            label = "Budget",
                            value = String.format("%.0f", purchaseStatistics.totalTarget),
                            valueColor = colors.primary,
                            modifier = Modifier.weight(1f)
                        )
                        UnifiedStatItem(
                            label = "Actual",
                            value = String.format("%.0f", purchaseStatistics.totalActual),
                            valueColor = getPurchaseAchievementColor(purchaseStatistics.achievementPercentage),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Purchase: Achievement & Categories On Target
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        UnifiedStatItem(
                            label = "Met",
                            value = if (purchaseStatistics.totalTarget > 0)
                                String.format("%.0f%%", purchaseStatistics.achievementPercentage) else "—",
                            valueColor = if (purchaseStatistics.totalTarget > 0)
                                getPurchaseAchievementColor(purchaseStatistics.achievementPercentage)
                            else colors.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        val onTargetPercentage = if (purchaseStatistics.totalCategories > 0)
                            (purchaseStatistics.categoriesOnTarget.toDouble() / purchaseStatistics.totalCategories) * 100
                        else 0.0

                        UnifiedStatItem(
                            label = "On Target",
                            value = "${purchaseStatistics.categoriesOnTarget} / ${purchaseStatistics.totalCategories}",
                            valueColor = when {
                                onTargetPercentage >= 80 -> Color(0xFF22C55E)    // SuccessGreen (good)
                                onTargetPercentage >= 50 -> Color(0xFFF59E0B)    // WarningAmber (warning)
                                else                     -> colors.error          // ErrorRed (poor)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun UnifiedStatItem(
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
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            fontSize = 15.sp
        )
    }
}
