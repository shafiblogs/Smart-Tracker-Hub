package com.marsa.smarttrackerhub.ui.screens.home

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

/**
 * Chart displaying target vs average sales for last 4-6 months
 *
 * @param data List of monthly data (max 6 months)
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
    var touchInfo by remember { mutableStateOf<ChartTouchInfo?>(null) }

    Box(modifier = modifier) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .pointerInput(data) {
                    detectTapGestures(
                        onTap = { offset ->
                            touchInfo = detectTouchedPoint(
                                offset = offset,
                                data = data,
                                chartWidth = size.width.toFloat(),
                                chartHeight = size.height.toFloat()
                            )
                        }
                    )
                }
        ) {

            if (data.isEmpty()) {
                drawEmptyState(colors)
                return@Canvas
            }

            val chartWidth = size.width
            val chartHeight = size.height
            val bottomPadding = 80f // Enough space for month labels
            val topPadding = 70f // Enough space for legend
            val leftPadding = 60f // Space for Y-axis labels
            val rightPadding = 30f
            val availableHeight = chartHeight - bottomPadding - topPadding
            val availableWidth = chartWidth - leftPadding - rightPadding

            // Calculate max value for Y-axis scaling
            val maxValue = data.maxOfOrNull { max(it.targetSale, it.averageSale) } ?: 1.0
            val yScale = availableHeight / maxValue.toFloat()

            // Fix for proper spacing: ensure we divide by (size - 1) for proper point distribution
            val xScale = if (data.size > 1) {
                availableWidth / (data.size - 1).toFloat()
            } else {
                availableWidth / 2f
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

            // Draw target line (dashed)
            drawTargetLine(data, leftPadding, chartHeight, bottomPadding, xScale, yScale, colors)

            // Draw average line (solid)
            drawAverageLine(data, leftPadding, chartHeight, bottomPadding, xScale, yScale, colors)

            data.forEachIndexed { index, monthData ->
                val x = leftPadding + (index * xScale)

                // Target point
                val targetY =
                    chartHeight - bottomPadding - (monthData.targetSale * yScale).toFloat()
                drawCircle(
                    color = Color(0xFF2196F3), // Blue
                    radius = 7f,
                    center = Offset(x, targetY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(x, targetY)
                )

                // Average point
                val avgY = chartHeight - bottomPadding - (monthData.averageSale * yScale).toFloat()
                val avgColor = if (monthData.isTargetMet) {
                    Color(0xFF4CAF50) // Green
                } else {
                    Color(0xFFF44336) // Red
                }

                drawCircle(
                    color = avgColor,
                    radius = 7f,
                    center = Offset(x, avgY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(x, avgY)
                )

                // Draw month labels (rotated)
                val labelY = chartHeight - bottomPadding + 30f

                drawContext.canvas.nativeCanvas.apply {
                    save()
                    rotate(-45f, x, labelY)
                    drawText(
                        monthData.monthShortName,
                        x,
                        labelY,
                        Paint().apply {
                            color = colors.onSurfaceVariant.toArgb()
                            textSize = 12.sp.toPx()
                            textAlign = Paint.Align.RIGHT
                        }
                    )
                    restore()
                }
            }

            // Draw legend
            drawLegend(chartWidth, topPadding, colors)

            // Show tooltip on touch
            touchInfo?.let { info ->
//                ChartTooltip(
//                    touchInfo = info,
//                    modifier = Modifier.align(Alignment.TopCenter)
//                )
            }
        }
    }
}


