package com.marsa.smarttrackerhub.ui.screens.investers

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttrackerhub.data.dao.SettlementEntryWithName
import com.marsa.smarttrackerhub.data.entity.YearEndSettlement
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Lists all past year-end settlements for a shop.
 * Each row is expandable to show per-investor entry details.
 * Entries with an outstanding balance show a "Mark as Paid" button.
 *
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun SettlementHistoryScreen(shopId: Int) {
    val viewModel: SettlementHistoryViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(shopId) { viewModel.init(context, shopId) }

    // "Mark as Paid" dialog — shown when dialogEntry is non-null
    uiState.dialogEntry?.let { entry ->
        MarkAsPaidDialog(
            entry = entry,
            onConfirm = { amount, date -> viewModel.markEntrySettled(entry, amount, date) },
            onDismiss = { viewModel.dismissMarkPaidDialog() }
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.settlements.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No settlements recorded yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Use the calculator to create one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }

                    item {
                        Text(
                            text = "Settlement History — ${uiState.shopName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(uiState.settlements, key = { it.id }) { settlement ->
                        val isExpanded = uiState.expandedSettlementId == settlement.id
                        SettlementHistoryCard(
                            settlement = settlement,
                            isExpanded = isExpanded,
                            entries = if (isExpanded) uiState.expandedEntries else emptyList(),
                            isLoadingEntries = isExpanded && uiState.isLoadingEntries,
                            onToggle = { viewModel.toggleSettlement(settlement.id) },
                            onMarkPaid = { entry -> viewModel.showMarkPaidDialog(entry) }
                        )
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

// ── Settlement card ───────────────────────────────────────────────────────

@Composable
private fun SettlementHistoryCard(
    settlement: YearEndSettlement,
    isExpanded: Boolean,
    entries: List<SettlementEntryWithName>,
    isLoadingEntries: Boolean,
    onToggle: () -> Unit,
    onMarkPaid: (SettlementEntryWithName) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row — always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateFormat.format(Date(settlement.settlementDate)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val periodStart = if (settlement.periodStartDate == 0L)
                        "Beginning"
                    else
                        dateFormat.format(Date(settlement.periodStartDate))
                    Text(
                        text = "Period: $periodStart → ${dateFormat.format(Date(settlement.settlementDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "AED ${String.format("%,.2f", settlement.totalInvested)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Total Invested",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expandable entries
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))

                    if (settlement.note.isNotBlank()) {
                        Text(
                            text = "Note: ${settlement.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    if (isLoadingEntries) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else {
                        entries.forEach { entry ->
                            SettlementEntryRow(
                                entry = entry,
                                onMarkPaid = { onMarkPaid(entry) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Per-investor entry row ────────────────────────────────────────────────

@Composable
private fun SettlementEntryRow(
    entry: SettlementEntryWithName,
    onMarkPaid: () -> Unit
) {
    val isOverpaid = entry.balanceAmount > 0
    val isBalanced = entry.balanceAmount == 0.0
    val isPaid     = entry.settlementPaidAmount > 0.0

    val balanceColor = when {
        isBalanced -> MaterialTheme.colorScheme.onSurfaceVariant
        isOverpaid -> MaterialTheme.colorScheme.primary    // overpaid → owed back
        else       -> MaterialTheme.colorScheme.error      // underpaid → owes
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Investor name + balance amount
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.investorName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = when {
                    isBalanced -> "Settled"
                    isOverpaid -> "+AED ${String.format("%,.2f", entry.balanceAmount)}"
                    else       -> "−AED ${String.format("%,.2f", kotlin.math.abs(entry.balanceAmount))}"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = balanceColor,
                fontSize = 13.sp
            )
        }

        // Fair share / actual paid sub-row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Fair: AED ${String.format("%,.2f", entry.fairShareAmount)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Paid: AED ${String.format("%,.2f", entry.actualPaidAmount)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Settlement payment status / action (only for non-balanced entries)
        if (!isBalanced) {
            Spacer(Modifier.height(6.dp))
            if (isPaid) {
                // Already marked as paid — show green status badge
                val paidDateStr = entry.settlementPaidDate?.let {
                    SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(it))
                } ?: ""
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Settled $paidDateStr · AED ${String.format("%,.2f", entry.settlementPaidAmount)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Outstanding — show action button
                OutlinedButton(
                    onClick = onMarkPaid,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Mark as Paid",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

// ── Mark as Paid dialog ───────────────────────────────────────────────────

@Composable
private fun MarkAsPaidDialog(
    entry: SettlementEntryWithName,
    onConfirm: (amount: Double, date: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val defaultAmount = kotlin.math.abs(entry.balanceAmount)
    var amountText by remember { mutableStateOf(String.format("%.2f", defaultAmount)) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Mark as Paid",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Record the settlement payment for ${entry.investorName}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                // Amount field — pre-filled with the outstanding balance
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { v ->
                        if (v.isEmpty() || v.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amountText = v
                            amountError = null
                        }
                    },
                    label = { Text("Amount (AED)") },
                    prefix = { Text("AED ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    isError = amountError != null,
                    supportingText = amountError?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                )

                Spacer(Modifier.height(12.dp))

                // Date field — pre-filled today, tappable date picker
                OutlinedTextField(
                    value = dateFormat.format(Date(selectedDate)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment Date") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val picked = Calendar.getInstance()
                                    picked.set(year, month, day, 0, 0, 0)
                                    picked.set(Calendar.MILLISECOND, 0)
                                    selectedDate = picked.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Pick payment date"
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount == null || amount <= 0.0) {
                    amountError = "Enter a valid amount"
                    return@Button
                }
                onConfirm(amount, selectedDate)
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
