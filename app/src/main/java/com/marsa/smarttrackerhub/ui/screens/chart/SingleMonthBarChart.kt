package com.marsa.smarttrackerhub.ui.screens.chart

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import kotlin.math.max

@Composable
fun SingleMonthBarChart(
    data: MonthlyChartData,
    colors: ColorScheme,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val chartWidth = size.width
        val chartHeight = size.height
        val bottomPadding = 80f
        val topPadding = 70f
        val leftPadding = 60f
        val rightPadding = 30f
        val availableHeight = chartHeight - bottomPadding - topPadding
        val availableWidth = chartWidth - leftPadding - rightPadding

        val maxValue = max(data.targetSale, data.averageSale)
        val yScale = availableHeight / maxValue.toFloat()

        // Calculate achievement percentage
        val achievementPercentage = (data.averageSale / data.targetSale) * 100

        // Helper function to get achievement color
        fun getAchievementColor(percentage: Double): Color {
            return when {
                percentage >= 100 -> Color(0xFF4CAF50) // Green - Target met
                percentage >= 90 -> Color(0xFFFF6F00)  // Brown/Orange - Close to target
                else -> Color(0xFFF44336)               // Red - Below target
            }
        }

        // Draw grid and Y-axis
        drawGridAndYAxis(
            maxValue,
            chartHeight,
            chartWidth,
            bottomPadding,
            topPadding,
            leftPadding,
            rightPadding,
            colors
        )

        // Bar width and spacing
        val totalBars = 2
        val totalSpacing = availableWidth * 0.7f
        val barWidth = (availableWidth - totalSpacing) / totalBars
        val spacing = totalSpacing / 3f

        // Calculate bar positions (centered)
        val startX = leftPadding + spacing
        val targetBarX = startX
        val avgBarX = targetBarX + barWidth + spacing

        // Draw Target bar
        val targetBarHeight = (data.targetSale * yScale).toFloat()
        drawRect(
            color = Color(0xFF2196F3),
            topLeft = Offset(
                targetBarX,
                chartHeight - bottomPadding - targetBarHeight
            ),
            size = Size(barWidth, targetBarHeight)
        )

        // Draw Average Sale bar with achievement-based color
        val avgBarHeight = (data.averageSale * yScale).toFloat()
        val avgColor = getAchievementColor(achievementPercentage)

        drawRect(
            color = avgColor,
            topLeft = Offset(
                avgBarX,
                chartHeight - bottomPadding - avgBarHeight
            ),
            size = Size(barWidth, avgBarHeight)
        )

        // Draw labels below bars
        val labelPaint = Paint().apply {
            color = colors.onSurface.toArgb()
            textSize = 13.sp.toPx()
            textAlign = Paint.Align.CENTER
        }

        drawContext.canvas.nativeCanvas.drawText(
            "Target",
            targetBarX + barWidth / 2,
            chartHeight - bottomPadding + 35f,
            labelPaint
        )

        drawContext.canvas.nativeCanvas.drawText(
            "Average Sale",
            avgBarX + barWidth / 2,
            chartHeight - bottomPadding + 35f,
            labelPaint
        )

        // Draw legend at top
        /*drawSingleMonthLegend(
            chartWidth,
            topPadding,
            colors,
            achievementPercentage
        )*/
    }
}