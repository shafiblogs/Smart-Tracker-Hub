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

@Composable
fun StatisticsCard(
    statistics: ChartStatistics,
    shopAddress: String,
    periodLabel: String,
    modifier: Modifier = Modifier,
    onShareClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val isTargetAchieved = statistics.averageAchievementPercentage >= 100

    // Helper function to get achievement color
    fun getAchievementColor(percentage: Double): Color {
        return when {
            percentage >= 100 -> Color(0xFF4CAF50) // Green - Target met
            percentage >= 90 -> Color(0xFFFF6F00)  // Brown/Orange - Close to target
            else -> Color(0xFFF44336)               // Red - Below target
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            Column {
                // Title row with share button alignment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartTitle(
                        shopAddress = shopAddress,
                        periodLabel = periodLabel,
                        isTargetAchieved = isTargetAchieved,
                        achievementPercentage = statistics.averageAchievementPercentage, // ADD this
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
                                contentDescription = "Share Statistics",
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // First row: Avg Target / Avg Sale
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        label = "Avg Target",
                        value = String.format(
                            "%.0f",
                            statistics.totalTarget / statistics.totalMonths
                        ),
                        valueColor = Color(0xFF2196F3)
                    )

                    StatItem(
                        label = "Avg Sale",
                        value = String.format(
                            "%.0f",
                            statistics.totalAverage / statistics.totalMonths
                        ),
                        valueColor = getAchievementColor(statistics.averageAchievementPercentage)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Second row: Achievement / Difference
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        label = "Achievement",
                        value = String.format("%.0f%%", statistics.averageAchievementPercentage),
                        valueColor = getAchievementColor(statistics.averageAchievementPercentage)
                    )

                    val difference = statistics.totalAverage - statistics.totalTarget
                    val icon = if (difference >= 0) "↑" else "↓"

                    StatItem(
                        label = "Difference",
                        value = "$icon ${String.format("%.0f", abs(difference))}",
                        valueColor = if (difference >= 0)
                            Color(0xFF4CAF50)
                        else
                            Color(0xFFF44336)
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