package com.marsa.smarttrackerhub.ui.screens.home


/**
 * Created by Muhammed Shafi on 12/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

import android.graphics.Paint
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Chart displaying target vs average sales for last 4 months
 *
 * @param data List of monthly data (max 4 months)
 * @param modifier Modifier for the chart
 * @param onShareClick Callback when share button is clicked
 */

fun detectTouchedPoint(
    offset: Offset,
    data: List<MonthlyChartData>,
    chartWidth: Float,
    chartHeight: Float
): ChartTouchInfo? {
    val bottomPadding = 80f
    val topPadding = 70f // Updated to match Canvas topPadding
    val leftPadding = 60f
    val rightPadding = 30f
    val availableHeight = chartHeight - bottomPadding - topPadding
    val availableWidth = chartWidth - leftPadding - rightPadding

    val maxValue = data.maxOfOrNull { max(it.targetSale, it.averageSale) } ?: return null
    val yScale = availableHeight / maxValue.toFloat()
    val xScale = if (data.size > 1) {
        availableWidth / (data.size - 1).toFloat()
    } else {
        availableWidth / 2f
    }

    // Larger touch radius for easier tapping
    val touchRadius = 50f

    var closestPoint: ChartTouchInfo? = null
    var closestDistance = Float.MAX_VALUE

    data.forEachIndexed { index, monthData ->
        val x = leftPadding + (index * xScale)

        // Check target point
        val targetY = chartHeight - bottomPadding - (monthData.targetSale * yScale).toFloat()
        val targetDist = sqrt((offset.x - x).pow(2) + (offset.y - targetY).pow(2))

        if (targetDist <= touchRadius && targetDist < closestDistance) {
            closestDistance = targetDist
            closestPoint = ChartTouchInfo(
                monthYear = monthData.monthYear,
                targetSale = monthData.targetSale,
                averageSale = monthData.averageSale,
                isTargetMet = monthData.isTargetMet
            )
        }

        // Check average point
        val avgY = chartHeight - bottomPadding - (monthData.averageSale * yScale).toFloat()
        val avgDist = sqrt((offset.x - x).pow(2) + (offset.y - avgY).pow(2))

        if (avgDist <= touchRadius && avgDist < closestDistance) {
            closestDistance = avgDist
            closestPoint = ChartTouchInfo(
                monthYear = monthData.monthYear,
                targetSale = monthData.targetSale,
                averageSale = monthData.averageSale,
                isTargetMet = monthData.isTargetMet
            )
        }
    }

    return closestPoint
}


/**
 * Draws grid lines and Y-axis labels
 */
fun DrawScope.drawGridAndYAxis(
    maxValue: Double,
    chartHeight: Float,
    chartWidth: Float,
    bottomPadding: Float,
    topPadding: Float,
    leftPadding: Float,
    rightPadding: Float,
    colors: ColorScheme
) {
    val gridLines = 5
    val step = maxValue / gridLines
    val availableHeight = chartHeight - bottomPadding - topPadding

    repeat(gridLines + 1) { i ->
        val value = step * i
        val y = chartHeight - bottomPadding - ((value / maxValue) * availableHeight).toFloat()

        // Draw horizontal grid line
        drawLine(
            color = colors.outlineVariant,
            start = Offset(leftPadding, y),
            end = Offset(chartWidth - rightPadding, y),
            strokeWidth = 1f
        )

        // Draw Y-axis value label
        // Move "0" label up slightly to avoid overlap with month names
        val labelY = if (i == 0) y - 5f else y + 5f

        drawContext.canvas.nativeCanvas.drawText(
            String.format("%.0f", value),
            leftPadding - 15f,
            labelY,
            Paint().apply {
                color = colors.onSurfaceVariant.toArgb()
                textSize = 11.sp.toPx()
                textAlign = Paint.Align.RIGHT
            }
        )
    }
}


/**
 * Draws target line (dashed)
 */
fun DrawScope.drawTargetLine(
    data: List<MonthlyChartData>,
    leftPadding: Float,
    chartHeight: Float,
    bottomPadding: Float,
    xScale: Float,
    yScale: Float,
    colors: ColorScheme
) {
    if (data.size < 2) return

    val path = Path()
    val firstY = chartHeight - bottomPadding - (data[0].targetSale * yScale).toFloat()
    path.moveTo(leftPadding, firstY)

    for (i in 1 until data.size) {
        val x = leftPadding + (i * xScale)
        val y = chartHeight - bottomPadding - (data[i].targetSale * yScale).toFloat()
        path.lineTo(x, y)
    }

    drawPath(
        path = path,
        color = colors.primary,
        style = Stroke(
            width = 3f,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )
    )
}


/**
 * Draws average sale line (solid, color per segment)
 */
fun DrawScope.drawAverageLine(
    data: List<MonthlyChartData>,
    leftPadding: Float,
    chartHeight: Float,
    bottomPadding: Float,
    xScale: Float,
    yScale: Float,
    colors: ColorScheme
) {
    if (data.size < 2) return

    // Draw each segment with color based on achievement
    for (i in 0 until data.size - 1) {
        val startX = leftPadding + (i * xScale)
        val startY = chartHeight - bottomPadding - (data[i].averageSale * yScale).toFloat()
        val endX = leftPadding + ((i + 1) * xScale)
        val endY = chartHeight - bottomPadding - (data[i + 1].averageSale * yScale).toFloat()

        // Color based on next point's achievement status
        val color = if (data[i + 1].isTargetMet) colors.tertiary else colors.error

        drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }
}


/**
 * Draws chart legend (centered at top)
 */
fun DrawScope.drawLegend(
    chartWidth: Float,
    topPadding: Float,
    colors: ColorScheme
) {
    val legendY = topPadding / 3
    val textSizePx = 13.sp.toPx()

    val paint = Paint().apply {
        color = colors.onSurface.toArgb()
        textSize = textSizePx
    }

    val targetText = "Target"
    val avgText = "Average Sale"
    val lineWidth = 30f
    val spacing = 60f

    // Measure text widths for proper centering
    val targetWidth = paint.measureText(targetText)
    val avgWidth = paint.measureText(avgText)

    val totalWidth = lineWidth + 5f + targetWidth + spacing + lineWidth + 5f + avgWidth

    val startX = (chartWidth - totalWidth) / 2

    // Draw Target legend
    drawLine(
        color = Color(0xFF2196F3),
        start = Offset(startX, legendY),
        end = Offset(startX + lineWidth, legendY),
        strokeWidth = 3f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
    )

    drawContext.canvas.nativeCanvas.drawText(
        targetText,
        startX + lineWidth + 5f,
        legendY + 5f,
        paint
    )

    // Draw Average legend
    val avgStart = startX + lineWidth + 5f + targetWidth + spacing
    drawLine(
        color = colors.tertiary,
        start = Offset(avgStart, legendY),
        end = Offset(avgStart + lineWidth, legendY),
        strokeWidth = 3f
    )

    drawContext.canvas.nativeCanvas.drawText(
        avgText,
        avgStart + lineWidth + 5f,
        legendY + 5f,
        paint
    )
}

/**
 * Draws month labels below the chart with proper spacing
 * FIXED: Increased spacing and proper alignment for 4-6 months
 */
fun DrawScope.drawMonthLabels(
    data: List<MonthlyChartData>,
    chartHeight: Float,
    bottomPadding: Float,
    leftPadding: Float,
    xScale: Float,
    colors: ColorScheme
) {
    val textSizePx = 12.sp.toPx()

    val paint = Paint().apply {
        color = colors.onSurface.toArgb()
        textSize = textSizePx
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    data.forEachIndexed { index, monthData ->
        val x = leftPadding + (index * xScale)

        // FIXED: Increased spacing from 18f to 35f for better separation from graph
        // This ensures labels don't overlap with the bottom line of the chart
        val labelY = chartHeight - bottomPadding + 35f

        drawContext.canvas.nativeCanvas.drawText(
            getShortMonthName(monthData.monthYear),
            x,
            labelY,
            paint
        )
    }
}


/**
 * Gets short month name from full monthYear string
 */
private fun getShortMonthName(monthYear: String): String {
    return try {
        val parts = monthYear.split(" - ")
        if (parts.isNotEmpty()) {
            val month = parts[0].trim()
            month.take(3) // First 3 letters
        } else {
            monthYear
        }
    } catch (e: Exception) {
        monthYear
    }
}


/**
 * Draws empty state message
 */
fun DrawScope.drawEmptyState(colors: ColorScheme) {
    drawContext.canvas.nativeCanvas.drawText(
        "No data available",
        size.width / 2,
        size.height / 2,
        Paint().apply {
            color = colors.onSurfaceVariant.toArgb()
            textSize = 16.sp.toPx()
            textAlign = Paint.Align.CENTER
        }
    )
}
