package com.marsa.smarttrackerhub.ui.screens.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ChartTitle(
    shopAddress: String,
    periodLabel: String = "",
    isTargetAchieved: Boolean = false,
    achievementPercentage: Double = 0.0, // NEW parameter
    modifier: Modifier = Modifier
) {
    if (shopAddress.isEmpty()) return

    val colors = MaterialTheme.colorScheme

    // Indicator color based on achievement percentage
    val indicatorColor = when {
        achievementPercentage >= 100 -> Color(0xFF4CAF50) // Green
        achievementPercentage >= 90 -> Color(0xFFFF6F00)  // Brown/Orange
        else -> Color(0xFFF44336)                          // Red
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
    ) {
        // Colored circle indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = indicatorColor,
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Shop address and period
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = shopAddress,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )

            if (periodLabel.isNotEmpty()) {
                Text(
                    text = " - ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSecondaryContainer
                )

                Text(
                    text = periodLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSecondaryContainer
                )
            }
        }
    }
}