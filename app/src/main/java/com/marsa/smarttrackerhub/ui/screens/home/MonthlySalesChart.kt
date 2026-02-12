package com.marsa.smarttrackerhub.ui.screens.home

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

/**
 * Chart displaying target vs average sales for last 4 months
 * 
 * @param data List of monthly data (max 4 months)
 * @param modifier Modifier for the chart
 * @param onShareClick Callback when share button is clicked
 */
@Composable
fun MonthlySalesChart(
    data: List<MonthlyChartData>,
    modifier: Modifier = Modifier,
    onShareClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    Box(modifier = modifier) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            if (data.isEmpty()) {
                drawEmptyState(colors)
                return@Canvas
            }

            val chartWidth = size.width
            val chartHeight = size.height
            val bottomPadding = 80f
            val topPadding = 70f
            val leftPadding = 60f
            val rightPadding = 30f
            val availableHeight = chartHeight - bottomPadding - topPadding
            val availableWidth = chartWidth - leftPadding - rightPadding

            val maxValue = data.maxOfOrNull { max(it.targetSale, it.averageSale) } ?: 1.0
            val yScale = availableHeight / maxValue.toFloat()
            val xScale =
                if (data.size > 1) availableWidth / (data.size - 1) else availableWidth / 2f

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

            drawTargetLine(data, leftPadding, chartHeight, bottomPadding, xScale, yScale, colors)

            drawAverageLine(data, leftPadding, chartHeight, bottomPadding, xScale, yScale, colors)

            // Draw points
            data.forEachIndexed { index, monthData ->
                val x = leftPadding + (index * xScale)

                val targetY =
                    chartHeight - bottomPadding - (monthData.targetSale * yScale).toFloat()

                drawCircle(
                    color = colors.primary,
                    radius = 8f,
                    center = Offset(x, targetY)
                )

                drawCircle(
                    color = colors.surface,
                    radius = 4f,
                    center = Offset(x, targetY)
                )

                val avgY =
                    chartHeight - bottomPadding - (monthData.averageSale * yScale).toFloat()

                val avgColor =
                    if (monthData.isTargetMet) colors.tertiary else colors.error

                drawCircle(
                    color = avgColor,
                    radius = 8f,
                    center = Offset(x, avgY)
                )

                drawCircle(
                    color = colors.surface,
                    radius = 4f,
                    center = Offset(x, avgY)
                )
            }

            drawLegend(chartWidth, topPadding, colors)
        }
    }
}


/**
 * Draws grid lines and Y-axis labels
 */
private fun DrawScope.drawGridAndYAxis(
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

        drawLine(
            color = colors.outlineVariant,
            start = Offset(leftPadding, y),
            end = Offset(chartWidth - rightPadding, y),
            strokeWidth = 1f
        )

        drawContext.canvas.nativeCanvas.drawText(
            String.format("%.0f", value),
            leftPadding - 15f,
            y + 5f,
            Paint().apply {
                color = colors.onSurfaceVariant.toArgb()
                textSize = 11.sp.toPx()
                textAlign = Paint.Align.RIGHT
            }
        )
    }
}


/**
 * Draws target line (dashed blue)
 */
private fun DrawScope.drawTargetLine(
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
private fun DrawScope.drawAverageLine(
    data: List<MonthlyChartData>,
    leftPadding: Float,
    chartHeight: Float,
    bottomPadding: Float,
    xScale: Float,
    yScale: Float,
    colors: ColorScheme
) {
    if (data.size < 2) return

    for (i in 0 until data.size - 1) {
        val startX = leftPadding + (i * xScale)
        val startY = chartHeight - bottomPadding - (data[i].averageSale * yScale).toFloat()
        val endX = leftPadding + ((i + 1) * xScale)
        val endY = chartHeight - bottomPadding - (data[i + 1].averageSale * yScale).toFloat()

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
private fun DrawScope.drawLegend(
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

    val targetWidth = paint.measureText(targetText)
    val avgWidth = paint.measureText(avgText)

    val totalWidth =
        lineWidth + 5f + targetWidth + spacing + lineWidth + 5f + avgWidth

    val startX = (chartWidth - totalWidth) / 2

    // Target
    drawLine(
        color = colors.primary,
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

    // Average
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
 * Draws empty state
 */
private fun DrawScope.drawEmptyState(colors: ColorScheme) {
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