package com.marsa.smarttrackerhub.ui.screens.chart

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import com.marsa.smarttrackerhub.utils.formatMoney

private data class AllocationSlice(val label: String, val amount: Double, val color: Color)

/**
 * Single segmented bar splitting Collection into Purchase, Expense, Withdrawal, Provision, and
 * Retained. A legend underneath shows each slice's % of collection and amount for all nonzero
 * slices. Out Payment is shown separately below a divider (only if nonzero) — it's not part of
 * the "where the money went" allocation, but a separate obligation.
 * Reuses the segmented-bar technique from [PurchaseCategoryChart] (Box + clip, weighted widths).
 */
@Composable
fun MoneyAllocationBar(
    collection: Double,
    purchase: Double,
    expense: Double,
    withdrawal: Double,
    provision: Double,
    outstandingPayments: Double = 0.0,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val status = semanticStatusColors()

    if (collection <= 0.0) {
        Text(
            text = "No collection recorded this month",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = modifier
        )
        return
    }

    val retained = (collection - purchase - expense - withdrawal - provision - outstandingPayments)
        .coerceAtLeast(0.0)

    // Bar slices: outflows are one hue stepped by opacity (100/72/52/36%), Retained is success green
    // This shows all outflows are the same thing (cash leaving) with only Retained picked out
    val outflowBase = colors.outline
    val slices = listOf(
        AllocationSlice("Purchase", purchase, outflowBase.copy(alpha = 1.0f)),
        AllocationSlice("Expense", expense, outflowBase.copy(alpha = 0.72f)),
        AllocationSlice("Withdrawal", withdrawal, outflowBase.copy(alpha = 0.52f)),
        AllocationSlice("Provision", provision, outflowBase.copy(alpha = 0.36f)),
        AllocationSlice("Retained", retained, status.success)
    ).filter { it.amount > 0 || it.label in listOf("Purchase", "Expense", "Withdrawal", "Provision") }

    Column(modifier = modifier.fillMaxWidth()) {
        // Segmented bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceVariant)
        ) {
            slices.forEach { slice ->
                Box(
                    modifier = Modifier
                        .weight((slice.amount / collection).toFloat().coerceAtLeast(0.001f))
                        .fillMaxHeight()
                        .background(slice.color)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend: colored dot + label + % + amount for all slices, right-aligned
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // "of collection" caption
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.width(16.dp))
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "of collection",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            slices.forEach { slice ->
                val pct = slice.amount / collection * 100.0
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(slice.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = slice.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${"%.0f".format(pct)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.width(35.dp),
                        textAlign = TextAlign.Right
                    )
                    Text(
                        text = formatMoney(slice.amount, 0),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = slice.color,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Right
                    )
                }
            }
        }

        // Divider + Out Payment row (only if Out Payment > 0)
        if (outstandingPayments > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = colors.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))
            AllocationRow("Out Payment", outstandingPayments, status.success)
        }
    }
}

/** Simple label + amount row (for Out Payment / Withdrawal / Provision when shown separately). */
@Composable
private fun AllocationRow(label: String, amount: Double, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatMoney(amount, 0),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}
