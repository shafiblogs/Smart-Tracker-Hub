package com.marsa.smarttrackerhub.ui.screens.home

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Chart displaying target vs average sales for last 6 months
 * 
 * @param data List of monthly data (max 6 months)
 * @param modifier Modifier for the chart
 */
@Composable
fun MonthlySalesChart(
    data: List<MonthlyChartData>,
    modifier: Modifier = Modifier
) {
    var touchInfo by remember { mutableStateOf<ChartTouchInfo?>(null) }
    
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .pointerInput(Unit) {
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
                drawEmptyState()
                return@Canvas
            }
            
            val chartWidth = size.width
            val chartHeight = size.height
            val bottomPadding = 80f
            val topPadding = 50f
            val leftPadding = 60f
            val rightPadding = 30f
            val availableHeight = chartHeight - bottomPadding - topPadding
            val availableWidth = chartWidth - leftPadding - rightPadding
            
            // Calculate max value for Y-axis scaling
            val maxValue = data.maxOfOrNull { max(it.targetSale, it.averageSale) } ?: 1.0
            val yScale = availableHeight / maxValue.toFloat()
            val xScale = if (data.size > 1) {
                availableWidth / (data.size - 1).toFloat()
            } else {
                availableWidth
            }
            
            // Draw grid and Y-axis
            drawGridAndYAxis(
                maxValue = maxValue,
                chartHeight = chartHeight,
                chartWidth = chartWidth,
                bottomPadding = bottomPadding,
                topPadding = topPadding,
                leftPadding = leftPadding,
                rightPadding = rightPadding
            )
            
            // Draw target line (dashed blue)
            drawTargetLine(
                data = data,
                leftPadding = leftPadding,
                chartHeight = chartHeight,
                bottomPadding = bottomPadding,
                xScale = xScale,
                yScale = yScale
            )
            
            // Draw average sale line (solid green/red)
            drawAverageLine(
                data = data,
                leftPadding = leftPadding,
                chartHeight = chartHeight,
                bottomPadding = bottomPadding,
                xScale = xScale,
                yScale = yScale
            )
            
            // Draw data points and month labels
            data.forEachIndexed { index, monthData ->
                val x = leftPadding + (index * xScale)
                
                // Target point
                val targetY = chartHeight - bottomPadding - (monthData.targetSale * yScale).toFloat()
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
                            color = android.graphics.Color.BLACK
                            textSize = 12.sp.toPx()
                            textAlign = Paint.Align.RIGHT
                        }
                    )
                    restore()
                }
            }
            
            // Draw legend
            drawLegend(
                chartWidth = chartWidth,
                topPadding = topPadding
            )
        }
        
        // Show tooltip on touch
        touchInfo?.let { info ->
            ChartTooltip(
                touchInfo = info,
                modifier = Modifier.align(Alignment.TopCenter)
            )
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
    rightPadding: Float
) {
    val gridLines = 5
    val step = maxValue / gridLines
    val availableHeight = chartHeight - bottomPadding - topPadding
    
    repeat(gridLines + 1) { i ->
        val value = step * i
        val y = chartHeight - bottomPadding - ((value / maxValue) * availableHeight).toFloat()
        
        // Draw horizontal grid line
        drawLine(
            color = Color(0xFFE0E0E0),
            start = Offset(leftPadding, y),
            end = Offset(chartWidth - rightPadding, y),
            strokeWidth = 1f
        )
        
        // Draw Y-axis value label
        drawContext.canvas.nativeCanvas.apply {
            drawText(
                String.format("%.0f", value),
                leftPadding - 15f,
                y + 5f,
                Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 11.sp.toPx()
                    textAlign = Paint.Align.RIGHT
                }
            )
        }
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
    yScale: Float
) {
    if (data.size < 2) {
        // For single point, just show the point (already drawn)
        return
    }
    
    val path = Path()
    val firstX = leftPadding
    val firstY = chartHeight - bottomPadding - (data[0].targetSale * yScale).toFloat()
    path.moveTo(firstX, firstY)
    
    for (i in 1 until data.size) {
        val x = leftPadding + (i * xScale)
        val y = chartHeight - bottomPadding - (data[i].targetSale * yScale).toFloat()
        path.lineTo(x, y)
    }
    
    drawPath(
        path = path,
        color = Color(0xFF2196F3), // Blue
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
    yScale: Float
) {
    if (data.size < 2) {
        return
    }
    
    // Draw each segment with color based on achievement
    for (i in 0 until data.size - 1) {
        val startX = leftPadding + (i * xScale)
        val startY = chartHeight - bottomPadding - (data[i].averageSale * yScale).toFloat()
        val endX = leftPadding + ((i + 1) * xScale)
        val endY = chartHeight - bottomPadding - (data[i + 1].averageSale * yScale).toFloat()
        
        // Color based on next point's achievement
        val color = if (data[i + 1].isTargetMet) {
            Color(0xFF4CAF50) // Green
        } else {
            Color(0xFFF44336) // Red
        }
        
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
 * Draws chart legend
 */
private fun DrawScope.drawLegend(
    chartWidth: Float,
    topPadding: Float
) {
    val legendY = topPadding / 2
    val legendStartX = chartWidth - 260f
    
    // Target legend
    drawLine(
        color = Color(0xFF2196F3),
        start = Offset(legendStartX, legendY),
        end = Offset(legendStartX + 30f, legendY),
        strokeWidth = 3f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
    )
    drawCircle(
        color = Color(0xFF2196F3),
        radius = 5f,
        center = Offset(legendStartX + 15f, legendY)
    )
    
    drawContext.canvas.nativeCanvas.apply {
        drawText(
            "Target",
            legendStartX + 40f,
            legendY + 5f,
            Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 13.sp.toPx()
            }
        )
    }
    
    // Average legend
    drawLine(
        color = Color(0xFF4CAF50),
        start = Offset(legendStartX + 110f, legendY),
        end = Offset(legendStartX + 140f, legendY),
        strokeWidth = 3f
    )
    drawCircle(
        color = Color(0xFF4CAF50),
        radius = 5f,
        center = Offset(legendStartX + 125f, legendY)
    )
    
    drawContext.canvas.nativeCanvas.apply {
        drawText(
            "Average Sale",
            legendStartX + 150f,
            legendY + 5f,
            Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 13.sp.toPx()
            }
        )
    }
}

/**
 * Draws empty state
 */
private fun DrawScope.drawEmptyState() {
    drawContext.canvas.nativeCanvas.apply {
        drawText(
            "No data available",
            size.width / 2,
            size.height / 2,
            Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 16.sp.toPx()
                textAlign = Paint.Align.CENTER
            }
        )
    }
}

/**
 * Detects touched point
 */
private fun detectTouchedPoint(
    offset: Offset,
    data: List<MonthlyChartData>,
    chartWidth: Float,
    chartHeight: Float
): ChartTouchInfo? {
    val bottomPadding = 80f
    val topPadding = 50f
    val leftPadding = 60f
    val rightPadding = 30f
    val availableHeight = chartHeight - bottomPadding - topPadding
    val availableWidth = chartWidth - leftPadding - rightPadding
    
    val maxValue = data.maxOfOrNull { max(it.targetSale, it.averageSale) } ?: return null
    val yScale = availableHeight / maxValue.toFloat()
    val xScale = if (data.size > 1) {
        availableWidth / (data.size - 1).toFloat()
    } else {
        availableWidth
    }
    
    val touchRadius = 40f
    
    data.forEachIndexed { index, monthData ->
        val x = leftPadding + (index * xScale)
        
        // Check target point
        val targetY = chartHeight - bottomPadding - (monthData.targetSale * yScale).toFloat()
        val targetDist = sqrt((offset.x - x).pow(2) + (offset.y - targetY).pow(2))
        
        if (targetDist <= touchRadius) {
            return ChartTouchInfo(
                monthYear = monthData.monthYear,
                targetSale = monthData.targetSale,
                averageSale = monthData.averageSale,
                isTargetMet = monthData.isTargetMet
            )
        }
        
        // Check average point
        val avgY = chartHeight - bottomPadding - (monthData.averageSale * yScale).toFloat()
        val avgDist = sqrt((offset.x - x).pow(2) + (offset.y - avgY).pow(2))
        
        if (avgDist <= touchRadius) {
            return ChartTouchInfo(
                monthYear = monthData.monthYear,
                targetSale = monthData.targetSale,
                averageSale = monthData.averageSale,
                isTargetMet = monthData.isTargetMet
            )
        }
    }
    
    return null
}

/**
 * Tooltip showing details
 */
@Composable
private fun ChartTooltip(
    touchInfo: ChartTouchInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = touchInfo.monthYear,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .padding(end = 8.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = Color(0xFF2196F3))
                        }
                    }
                    Text(
                        text = "Target:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                Text(
                    text = String.format("%.2f", touchInfo.targetSale),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Average
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .padding(end = 8.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = if (touchInfo.isTargetMet) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }
                    }
                    Text(
                        text = "Average:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                Text(
                    text = String.format("%.2f", touchInfo.averageSale),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (touchInfo.isTargetMet) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            
            // Achievement %
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Achievement:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = String.format("%.1f%%", touchInfo.achievementPercentage),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (touchInfo.isTargetMet) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
            
            // Difference
            val icon = if (touchInfo.difference >= 0) "↑" else "↓"
            val diffColor = if (touchInfo.difference >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "$icon ${String.format("%.2f", abs(touchInfo.difference))}",
                style = MaterialTheme.typography.bodyMedium,
                color = diffColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
