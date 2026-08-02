package com.marsa.smarttrackerhub.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marsa.smarttracker.ui.theme.semanticStatusColors
import kotlin.math.abs

/**
 * Month-over-month percentage change, or null when there's no meaningful baseline
 * (previous value is zero → an infinite/undefined change we shouldn't render).
 */
fun percentChange(current: Double, previous: Double): Double? =
    if (previous == 0.0) null else (current - previous) / abs(previous) * 100.0

/**
 * Compact "at a glance" delta pill: ▲/▼ + percentage, colored by whether the move is good.
 * Renders nothing when [deltaPercent] is null. [higherIsBetter] flips the good/bad colouring
 * (true for sales/profit/cash). A near-flat move (<0.5%) shows neutral with no arrow.
 */
@Composable
fun DeltaChip(
    deltaPercent: Double?,
    modifier: Modifier = Modifier,
    label: String? = null,
    higherIsBetter: Boolean = true
) {
    if (deltaPercent == null) return
    val status = semanticStatusColors()
    val flat = abs(deltaPercent) < 0.5
    val up = deltaPercent > 0
    val good = if (higherIsBetter) up else !up
    val color = when {
        flat -> MaterialTheme.colorScheme.onSurfaceVariant
        good -> status.success
        else -> status.danger
    }
    val icon = when {
        flat -> null
        up -> Icons.Default.KeyboardArrowUp
        else -> Icons.Default.KeyboardArrowDown
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (label != null) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = color)
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            }
            Text(
                text = "${"%.0f".format(abs(deltaPercent))}%",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = color
            )
        }
    }
}
