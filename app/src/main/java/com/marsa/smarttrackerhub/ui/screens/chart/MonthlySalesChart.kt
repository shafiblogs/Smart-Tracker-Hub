package com.marsa.smarttrackerhub.ui.screens.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun MonthlySalesChart(
    data: List<MonthlyChartData>,
    shopAddress: String = "",
    periodLabel: String = "",
    isTargetAchieved: Boolean = false,
    modifier: Modifier = Modifier,
    onShareClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    var touchInfo by remember { mutableStateOf<ChartTouchInfo?>(null) }

    Box(modifier = modifier) {
        Column {
            // Title section
            if (shopAddress.isNotEmpty()) {
                ChartTitle(
                    shopAddress = shopAddress,
                    periodLabel = periodLabel,
                    isTargetAchieved = isTargetAchieved
                )
            }

            // Chart Canvas - Choose based on data size
            if (data.size == 1) {
                // Single month - use bar chart
                SingleMonthBarChart(
                    data = data.first(),
                    colors = colors,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            } else {
                // Multiple months - use existing line chart
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
                    // Your existing multi-month line chart code
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

                    val xScale = if (data.size > 1) {
                        availableWidth / (data.size - 1).toFloat()
                    } else {
                        availableWidth / 2f
                    }

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

                    drawTargetLine(
                        data,
                        leftPadding,
                        chartHeight,
                        bottomPadding,
                        xScale,
                        yScale,
                        colors
                    )
                    drawAverageLine(
                        data,
                        leftPadding,
                        chartHeight,
                        bottomPadding,
                        xScale,
                        yScale,
                        colors
                    )

                    data.forEachIndexed { index, monthData ->
                        val x = leftPadding + (index * xScale)

                        val targetY =
                            chartHeight - bottomPadding - (monthData.targetSale * yScale).toFloat()
                        drawCircle(
                            color = Color(0xFF2196F3),
                            radius = 7f,
                            center = Offset(x, targetY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3f,
                            center = Offset(x, targetY)
                        )

                        val avgY =
                            chartHeight - bottomPadding - (monthData.averageSale * yScale).toFloat()
                        val avgColor = if (monthData.isTargetMet) {
                            Color(0xFF4CAF50)
                        } else {
                            Color(0xFFF44336)
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
                    }

                    drawMonthLabels(
                        data = data,
                        chartHeight = chartHeight,
                        bottomPadding = bottomPadding,
                        leftPadding = leftPadding,
                        xScale = xScale,
                        colors = colors
                    )

                    //drawLegend(chartWidth, topPadding, colors)
                }
            }
        }
    }
}