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
import com.marsa.smarttrackerhub.domain.ChartStatistics
import kotlin.math.abs

/**
 * Statistics summary card with share functionality
 */
@Composable
fun StatisticsCard(
    statistics: ChartStatistics,
    shopAddress: String,
    periodLabel: String, // NEW: Dynamic period label
    modifier: Modifier = Modifier,
    onShareClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.secondaryContainer
        )
    ) {
        Box {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Title with dynamic period
                Text(
                    text = "$shopAddress - $periodLabel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // First row: Avg Target / Avg Sale
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        label = "Avg Target",
                        value = String.format("%.0f", statistics.totalTarget / statistics.totalMonths),
                        valueColor = colors.onSecondaryContainer
                    )

                    StatItem(
                        label = "Avg Sale",
                        value = String.format("%.0f", statistics.totalAverage / statistics.totalMonths),
                        valueColor = if (statistics.averageAchievementPercentage >= 100)
                            colors.tertiary else colors.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Second row: Achievement / Difference (instead of Targets)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        label = "Achievement",
                        value = String.format("%.0f%%", statistics.averageAchievementPercentage),
                        valueColor = if (statistics.averageAchievementPercentage >= 100)
                            colors.tertiary else colors.error
                    )

                    // Calculate difference between average and target
                    val difference = statistics.totalAverage - statistics.totalTarget
                    val icon = if (difference >= 0) "↑" else "↓"

                    StatItem(
                        label = "Difference",
                        value = "$icon ${String.format("%.0f", abs(difference))}",
                        valueColor = if (difference >= 0) colors.tertiary else colors.error
                    )
                }
            }

            // Share button at top-right
            onShareClick?.let { callback ->
                IconButton(
                    onClick = callback,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Statistics",
                        tint = colors.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
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