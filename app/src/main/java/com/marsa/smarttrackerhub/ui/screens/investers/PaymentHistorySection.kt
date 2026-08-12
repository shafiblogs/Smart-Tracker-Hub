package com.marsa.smarttrackerhub.ui.screens.investers

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marsa.smarttrackerhub.domain.PhaseTransactionDetail
import com.marsa.smarttrackerhub.ui.components.AedText
import com.marsa.smarttrackerhub.ui.components.DetailSectionCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "Payment History" — one [DetailSectionCard] holding every phase group, divider-separated,
 * instead of a stack of individually-elevated phase cards.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaymentHistorySection(
    transactions: List<PhaseTransactionDetail>,
    isAdmin: Boolean,
    onEditTransaction: (PhaseTransactionDetail) -> Unit
) {
    DetailSectionCard(title = "Payment History") {
        if (transactions.isEmpty()) {
            Text(
                text = "No payments recorded yet. Tap 'Record Payment' to add one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val grouped = transactions.groupBy { it.phase }.entries.toList()
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                grouped.forEachIndexed { index, (phase, txList) ->
                    PhaseGroup(phase, txList, isAdmin, onEditTransaction)
                    if (index < grouped.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhaseGroup(
    phase: String,
    transactions: List<PhaseTransactionDetail>,
    isAdmin: Boolean,
    onEditTransaction: (PhaseTransactionDetail) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    val phaseTotal = transactions.sumOf { it.amount }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = phase,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            AedText(
                amount = phaseTotal,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        transactions.forEach { tx ->
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { if (isAdmin) onEditTransaction(tx) }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.investorName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateFormat.format(Date(tx.transactionDate)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (tx.note.isNotBlank()) {
                        Text(
                            text = tx.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isAdmin) {
                        Text(
                            text = "Hold to edit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                    }
                }
                AedText(
                    amount = tx.amount,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
