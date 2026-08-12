package com.marsa.smarttrackerhub.ui.screens.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.marsa.smarttracker.ui.theme.semanticStatusColors
import com.marsa.smarttrackerhub.domain.AccountSummary
import kotlin.math.max

/**
 * Gross vs Net Profit across the last few months. Reuses the shared axis/grid/label drawing
 * from [ChartUtils] ([drawGridAndYAxis], [drawTargetLine], [drawMonthLabels], [drawEmptyState])
 * by mapping each month onto a [MonthlyChartData] (targetSale = Gross Profit, so it draws as the
 * dashed reference line). Net Profit is drawn as its own solid, sign-coloured line since it can
 * go negative in a way the shared "achievement" line isn't built for.
 *
 * [history] must be oldest → newest (left → right on the chart).
 */
@Composable
fun AccountTrendChart(history: List<AccountSummary>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val status = semanticStatusColors()

    if (history.size < 2) return

    val chartData = history.map {
        MonthlyChartData(monthYear = it.monthYear, monthShortName = "", targetSale = it.grossProfit, averageSale = it.netProfit)
    }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(colors.primary); Text(" Gross  ", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            LegendDot(status.success); Text(" Net", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .height(200.dp)
                .padding(top = 8.dp)
        ) {
            if (chartData.isEmpty()) {
                drawEmptyState(colors)
                return@Canvas
            }

            val chartWidth = size.width
            val chartHeight = size.height
            val bottomPadding = 40f
            val topPadding = 20f
            val leftPadding = 60f
            val rightPadding = 20f
            val availableHeight = chartHeight - bottomPadding - topPadding
            val availableWidth = chartWidth - leftPadding - rightPadding

            val maxValue = max(1.0, chartData.maxOf { max(it.targetSale, it.averageSale) })
            val yScale = availableHeight / maxValue.toFloat()
            val xScale = if (chartData.size > 1) availableWidth / (chartData.size - 1).toFloat() else availableWidth / 2f

            drawGridAndYAxis(maxValue, chartHeight, chartWidth, bottomPadding, topPadding, leftPadding, rightPadding, colors)
            drawTargetLine(chartData, leftPadding, chartHeight, bottomPadding, xScale, yScale, colors)

            // Net Profit — solid line, segment-coloured by sign (unlike drawAverageLine, which
            // colours by "target met", meaningless here since net rarely exceeds gross).
            for (i in 0 until chartData.size - 1) {
                val startX = leftPadding + (i * xScale)
                val startY = chartHeight - bottomPadding - (chartData[i].averageSale * yScale).toFloat()
                val endX = leftPadding + ((i + 1) * xScale)
                val endY = chartHeight - bottomPadding - (chartData[i + 1].averageSale * yScale).toFloat()
                val segColor = if (chartData[i + 1].averageSale >= 0) status.success else status.danger
                drawLine(
                    color = segColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
            chartData.forEachIndexed { index, month ->
                val x = leftPadding + (index * xScale)
                val netY = chartHeight - bottomPadding - (month.averageSale * yScale).toFloat()
                val netColor = if (month.averageSale >= 0) status.success else status.danger
                drawCircle(color = netColor, radius = 6f, center = Offset(x, netY))
                drawCircle(color = colors.surface, radius = 2.5f, center = Offset(x, netY))
            }

            drawMonthLabels(chartData, chartHeight, bottomPadding, leftPadding, xScale, colors)
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color) {
    Canvas(modifier = Modifier.width(8.dp).height(8.dp)) {
        drawCircle(color = color, radius = size.minDimension / 2)
    }
}
