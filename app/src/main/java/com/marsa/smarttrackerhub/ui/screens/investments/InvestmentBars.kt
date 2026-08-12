package com.marsa.smarttrackerhub.ui.screens.investments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.marsa.smarttracker.ui.theme.semanticStatusColors

/** Top-N shops get their own segment; everything past that is aggregated into one muted tail. */
private const val NAMED_SEGMENTS = 4

/**
 * Segmented bar showing how portfolio capital is split across shops — biggest shop first, in the
 * darkest shade, fading through a single-hue ramp. Shops past [NAMED_SEGMENTS] are pooled into a
 * final muted segment so the bar always represents 100% of the portfolio.
 *
 * The ramp is built by lerping the base colour toward [surfaceVariant] rather than by dropping
 * alpha or reaching for `on*` roles — `on*` colours are foregrounds (near-white here) and render
 * invisible when used as fills, and alpha-fading a single grey gives no real shade separation.
 */
@Composable
fun CapitalDistributionBar(
    shops: List<InvestmentShopRow>,
    totalCapital: Double,
    modifier: Modifier = Modifier
) {
    if (totalCapital <= 0 || shops.isEmpty()) return

    val fadeTarget = MaterialTheme.colorScheme.surfaceVariant
    val base = semanticStatusColors().success

    val named = shops.take(NAMED_SEGMENTS)
    val pooledCapital = shops.drop(NAMED_SEGMENTS).sumOf { it.totalCapital }
    val segmentCount = named.size + if (pooledCapital > 0) 1 else 0

    // Descending shades of one hue, spread across however many segments actually exist — dividing
    // by a fixed max instead would leave a 2–3 shop portfolio with near-identical shades.
    fun shade(index: Int): Color =
        if (segmentCount <= 1) base
        else lerp(base, fadeTarget, (index.toFloat() / (segmentCount - 1)) * 0.72f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            named.forEachIndexed { index, shop ->
                Segment(
                    fraction = (shop.totalCapital / totalCapital).toFloat(),
                    color = shade(index)
                )
                if (index < named.lastIndex || pooledCapital > 0) {
                    Spacer(modifier = Modifier.width(2.dp))
                }
            }
            if (pooledCapital > 0) {
                Segment(
                    fraction = (pooledCapital / totalCapital).toFloat(),
                    color = shade(segmentCount - 1)
                )
            }
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = "Capital split across shops",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** One slice of the distribution bar. Weight is floored so a tiny shop still shows a sliver. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.Segment(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .weight(fraction.coerceIn(0.01f, 1f))
            .fillMaxHeight()
            .background(color)
    )
}

/**
 * How much of the portfolio this one shop holds, as a bar.
 *
 * This deliberately visualises capital share rather than allocation %: investor shares are
 * expected to sum to 100 for every shop, so an allocation bar would render full on every card and
 * carry no information. Capital share is the quantity that actually varies, and drawing it lets
 * you rank shops by scanning bar lengths down the list instead of diffing formatted money.
 */
@Composable
fun PortfolioShareBar(
    sharePercentage: Double,
    modifier: Modifier = Modifier
) {
    val filled = (sharePercentage.coerceIn(0.0, 100.0) / 100.0).toFloat()
    val status = semanticStatusColors()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        if (filled > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(filled)
                    .fillMaxHeight()
                    .background(status.success)
            )
        }
    }
}

/**
 * Allocation is expected to be exactly 100%, so it's surfaced only when it *isn't* — amber when
 * equity is still unassigned, red when the shares sum past 100 (a data error worth seeing).
 * Returns null at exactly 100 so the caller can omit the row entirely.
 */
@Composable
fun allocationAnomalyColor(allocatedPercentage: Double): Color? {
    val status = semanticStatusColors()
    return when {
        allocatedPercentage > 100.0 -> status.danger
        allocatedPercentage < 100.0 -> status.warning
        else -> null
    }
}
