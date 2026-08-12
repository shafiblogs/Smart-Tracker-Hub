package com.marsa.smarttrackerhub.ui.screens.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marsa.smarttracker.ui.theme.semanticStatusColors

/**
 * Gross-vs-Net profit donut for the Home account card.
 *
 * Net profit is a *slice* of gross profit (gross − expenses − withdrawal − provision = net), so
 * this is drawn as one part-to-whole circle rather than two independent rings:
 *
 *   whole circle    = Gross Profit
 *   green/red arc   = Net Profit — the part actually kept (sign-coloured: green if net ≥ 0,
 *                     red if net < 0)
 *   neutral remainder = what gross lost on the way to net (expenses / withdrawal / provision)
 *   centre          = net as a % of gross (no caption — the surrounding tiles already say
 *                     "Gross Profit" / "Net Profit", so the number alone is unambiguous here)
 *
 * The remainder is deliberately a neutral grey, not a red tint: the "kept" arc already uses red
 * for a loss month, and a red-tinted remainder next to a red "kept" arc reads as one colour.
 * Neutral vs sign-coloured is unambiguous in every case.
 *
 * The split moves every month, so the ring always carries information — unlike a ring whose
 * outer track is a fixed full circle.
 *
 * Edge cases: gross ≤ 0 → empty track + "—" (no whole to take a slice of). Net ≤ 0 → no green/red
 * arc is drawn at all (0% of the whole is "kept") and the centre shows the negative %. Net > gross
 * (other income) → the arc caps at a full circle while the centre still reports the true >100%.
 */
@Composable
fun ProfitComparisonRing(
    grossProfit: Double,
    netProfit: Double,
    modifier: Modifier = Modifier,
    diameter: Dp = 76.dp
) {
    val colors = MaterialTheme.colorScheme
    val status = semanticStatusColors()

    val hasGross = grossProfit > 0
    val retention = if (hasGross) netProfit / grossProfit else null
    val netFraction = retention?.coerceIn(0.0, 1.0)?.toFloat() ?: 0f
    val netColor = if (netProfit >= 0) status.success else status.danger
    // Always neutral — never red/green — so it can never be confused with the sign-coloured
    // "kept" arc, even on a loss month where that arc is red too.
    val lostColor = colors.surfaceVariant

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = Stroke(width = size.minDimension * 0.18f, cap = StrokeCap.Butt)
            val inset = stroke.width / 2f
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // Whole circle = Gross Profit (shown in the "lost" tint; the net arc paints over it).
            drawArc(
                color = lostColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            // Net Profit slice.
            if (hasGross && netFraction > 0f) {
                drawArc(
                    color = netColor,
                    startAngle = -90f,
                    sweepAngle = 360f * netFraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
            }
        }
        Text(
            text = retention?.let { "${"%.0f".format(it * 100)}%" } ?: "—",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (hasGross) netColor else colors.onSurfaceVariant
        )
    }
}
