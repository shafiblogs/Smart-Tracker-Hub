package com.marsa.smarttrackerhub.ui.screens.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marsa.smarttracker.ui.theme.semanticStatusColors
import com.marsa.smarttrackerhub.ui.components.AedText

private data class WaterfallStep(
    val label: String,
    val delta: Double,
    val runningBefore: Double,
    val isTerminal: Boolean = false
)

/**
 * Horizontal waterfall: Opening Cash → +Collection → −Purchase → −Expense → −Withdrawal →
 * Closing Cash. Each floating bar spans its own contribution to the running cash total;
 * the two terminal bars (Opening/Closing) are anchored to the shared baseline.
 */
@Composable
fun CashFlowWaterfallChart(
    openingCash: Double,
    collection: Double,
    purchase: Double,
    expense: Double,
    withdrawal: Double,
    closingCash: Double,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val status = semanticStatusColors()

    val steps = buildList {
        var running = openingCash
        add(WaterfallStep("Opening", openingCash, 0.0, isTerminal = true))
        add(WaterfallStep("Collection", collection, running)); running += collection
        add(WaterfallStep("Purchase", -purchase, running)); running -= purchase
        add(WaterfallStep("Expense", -expense, running)); running -= expense
        add(WaterfallStep("Withdrawal", -withdrawal, running)); running -= withdrawal
        add(WaterfallStep("Closing", closingCash, 0.0, isTerminal = true))
    }

    val runningValues = steps.filter { !it.isTerminal }
        .flatMap { listOf(it.runningBefore, it.runningBefore + it.delta) }
    val allValues = runningValues + listOf(0.0, openingCash, closingCash)
    val maxValue = (allValues.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val minValue = (allValues.minOrNull() ?: 0.0).coerceAtMost(0.0)
    val range = (maxValue - minValue).coerceAtLeast(1.0)
    fun fractionOf(v: Double): Float = ((v - minValue) / range).toFloat().coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        steps.forEach { step ->
            val barColor = when {
                step.isTerminal -> colors.primary
                step.delta >= 0 -> status.success
                else -> status.danger
            }
            val startFraction = if (step.isTerminal) 0f else fractionOf(minOf(step.runningBefore, step.runningBefore + step.delta))
            val endFraction = if (step.isTerminal) fractionOf(step.delta) else fractionOf(maxOf(step.runningBefore, step.runningBefore + step.delta))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = step.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(78.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BoxWithConstraints(modifier = Modifier.weight(1f).height(14.dp)) {
                    val trackWidth = maxWidth
                    val startX = trackWidth * startFraction
                    val barWidth = (trackWidth * (endFraction - startFraction)).coerceAtLeast(2.dp)
                    Box(
                        modifier = Modifier
                            .offset(x = startX)
                            .width(barWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                AedText(
                    amount = step.delta,
                    prefix = if (!step.isTerminal && step.delta >= 0) "+" else "",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = barColor,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(84.dp)
                )
            }
        }
    }
}
