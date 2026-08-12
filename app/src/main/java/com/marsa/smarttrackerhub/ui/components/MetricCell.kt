package com.marsa.smarttrackerhub.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * One "label value" pair taking equal width, so two cells per row line up into columns
 * across rows. Used by the Sales and Account month-list row metrics.
 */
@Composable
fun RowScope.MetricCell(label: String, value: String, valueColor: Color) {
    MetricCellChrome(label) {
        MoneyText(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor,
            maxLines = 1
        )
    }
}

/** Money variant — renders the AED glyph via [AedText] instead of a plain formatted string. */
@Composable
fun RowScope.MetricCell(label: String, amount: Double, decimals: Int = 0, valueColor: Color) {
    MetricCellChrome(label) {
        AedText(
            amount = amount,
            decimals = decimals,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor,
            maxLines = 1
        )
    }
}

@Composable
private fun RowScope.MetricCellChrome(label: String, value: @Composable () -> Unit) {
    Row(
        modifier = Modifier.weight(1f).padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(6.dp))
        value()
    }
}
