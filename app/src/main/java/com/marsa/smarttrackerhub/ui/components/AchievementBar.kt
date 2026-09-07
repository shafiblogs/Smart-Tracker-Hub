package com.marsa.smarttrackerhub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Unclamped achievement bar with a fixed target tick — the fill runs past the tick when
 * actual exceeds target, instead of the bar simply looking "full" at 100%. Shared by the
 * Home statistics card and the Sales detail screen so the same month reads identically
 * on both.
 */
@Composable
fun AchievementBar(
    actual: Double,
    target: Double,
    color: Color,
    label: String = "target",
    modifier: Modifier = Modifier
) {
    // max(actual, target) is NaN whenever either input is NaN (0/0 upstream), which then makes
    // every derived fraction NaN too — coerceIn treats NaN as "larger than max" and silently
    // clamps to 1.0, so guard the inputs before they reach kotlin.math.max.
    val safeActual = actual.takeIf { it.isFinite() } ?: 0.0
    val safeTarget = target.takeIf { it.isFinite() } ?: 0.0
    val maxValue = kotlin.math.max(safeActual, safeTarget).coerceAtLeast(1.0)
    val filledFraction = (safeActual / maxValue).coerceIn(0.0, 1.0).toFloat()
    val targetFraction = (safeTarget / maxValue).coerceIn(0.0, 1.0).toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            // Filled portion
            if (filledFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(filledFraction)
                        .fillMaxHeight()
                        .background(color)
                )
            }
            // Target tick mark
            Box(
                modifier = Modifier
                    .fillMaxWidth(targetFraction)
                    .fillMaxHeight()
                    .padding(end = 1.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // RowScope.weight requires a value strictly > 0 — clamp both sides away from the
            // 0/1 extremes the tick position can legitimately hit (no target, or target ≥ actual).
            val leadFraction = targetFraction.coerceIn(0.0001f, 0.9999f)
            Spacer(modifier = Modifier.weight(leadFraction))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f - leadFraction))
        }
    }
}
