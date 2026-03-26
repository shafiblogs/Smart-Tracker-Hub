package com.marsa.smarttrackerhub.ui.screens.purchase

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Divider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marsa.smarttrackerhub.ui.screens.sale.MonthItem
import com.marsa.smarttrackerhub.utils.formatTimestamp

/**
 * Card showing one month's category-wise purchase breakdown.
 * Tapping the header expands/collapses the breakdown table.
 *
 * Created by Muhammed Shafi on 13/03/2026.
 * Moro Hub
 */
@Composable
fun PurchaseCard(
    monthItem: MonthItem,
    isSelected: Boolean,
    purchases: List<PurchaseItem>?,   // null = not yet loaded; emptyList = loaded but no data
    isLoading: Boolean,
    shopAddress: String,
    lastUpdated: Long = 0L,
    onClick: () -> Unit,
    onRefresh: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header row — always visible ─────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = monthItem.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (shopAddress.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = shopAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (isSelected) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isSelected) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // ── Expanded content ────────────────────────────────────────
            if (isSelected) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    purchases != null -> {
                        // Updated timestamp + share + refresh
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Updated: ${lastUpdated.formatTimestamp()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                onShare?.let { shareCallback ->
                                    IconButton(
                                        onClick = shareCallback,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = onRefresh,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Refresh", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))

                        if (purchases.isEmpty()) {
                            Text(
                                text = "No purchase data for this month",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            PurchaseBreakdownTable(purchases = purchases)
                        }
                    }

                    else -> {
                        Text(
                            text = "Failed to load purchase data",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Breakdown table ──────────────────────────────────────────────────────────

@Composable
private fun PurchaseBreakdownTable(purchases: List<PurchaseItem>) {
    Column {
        // Column headers
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }

        Divider(modifier = Modifier.padding(vertical = 6.dp))

        // Data rows
        purchases.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Đ%.2f".format(item.totalAmount),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 6.dp))

        // Total row
        val total = purchases.sumOf { it.totalAmount }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Đ%.2f".format(total),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
