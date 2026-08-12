package com.marsa.smarttrackerhub.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


/**
 * Created by Muhammed Shafi on 31/05/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

/**
 * [showZero] defaults to true so the row set is fixed month to month — a hidden zero is
 * indistinguishable from missing data. Pass false to keep the old hide-when-zero behaviour.
 * A zero amount renders in [MaterialTheme.colorScheme.onSurfaceVariant] regardless of [color],
 * so it reads as "nothing happened" rather than a live figure.
 */
@Composable
fun InfoRow(
    label: String,
    amount: Double,
    color: Color = MaterialTheme.colorScheme.onSurface,
    showZero: Boolean = true
) {
    if (!showZero && amount == 0.0) return
    val rowColor = if (amount == 0.0) MaterialTheme.colorScheme.onSurfaceVariant else color
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = rowColor)
        if (label.contains("Margin")) {
            Text(
                text = "${"%.2f".format(amount)}%",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = rowColor
            )
        } else {
            AedText(
                amount = amount,
                decimals = 2,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = rowColor
            )
        }
    }
}

@Composable
fun InfoRow(value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = color
        )
    }
}