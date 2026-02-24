package com.marsa.smarttrackerhub.ui.screens.investers

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttrackerhub.domain.InvestorSettlementRow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Settlement Calculator for a specific shop.
 *
 * Covers only the period since the last settlement (period-based).
 * The user picks the settlement date via a date picker.
 *
 * Shows:
 *  - Period range (from last settlement date → chosen date)
 *  - Total invested in this period
 *  - Per-investor breakdown: fair share vs actual paid vs balance
 *  - Optional note
 *  - Confirm button → saves YearEndSettlement + SettlementEntry rows
 *
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun SettlementCalculatorScreen(
    shopId: Int,
    onSettlementSaved: () -> Unit
) {
    val viewModel: SettlementCalculatorViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    LaunchedEffect(shopId) { viewModel.init(context, shopId) }
    LaunchedEffect(uiState.saveSuccess) { if (uiState.saveSuccess) onSettlementSaved() }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.rows.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No data for this period",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Record at least one payment transaction before running a settlement.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }

                    // ── Period + Total Capital Card ──
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = uiState.shopName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(4.dp))
                                // Period range label
                                val periodStart = if (uiState.periodStartDate == 0L)
                                    "Beginning"
                                else
                                    dateFormat.format(Date(uiState.periodStartDate))
                                val periodEnd = dateFormat.format(Date(uiState.settlementDate))
                                Text(
                                    text = "Period: $periodStart → $periodEnd",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Total Invested This Period",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "AED ${String.format("%,.2f", uiState.totalInvested)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // ── Settlement Date Picker ──
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Settlement Date",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = dateFormat.format(Date(uiState.settlementDate)),
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val cal = Calendar.getInstance().apply {
                                            timeInMillis = uiState.settlementDate
                                        }
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, day ->
                                                val picked = Calendar.getInstance()
                                                picked.set(year, month, day, 23, 59, 59)
                                                picked.set(Calendar.MILLISECOND, 999)
                                                viewModel.onSettlementDateChange(picked.timeInMillis)
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = "Pick settlement date"
                                        )
                                    }
                                },
                                supportingText = {
                                    Text(
                                        text = "Transactions up to this date are included in the settlement.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }

                    // ── Per-Investor Breakdown ──
                    item {
                        Text(
                            text = "Investor Settlement",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(uiState.rows) { row -> SettlementRowCard(row = row) }

                    // ── Who owes whom summary ──
                    item { SettlementSummaryCard(rows = uiState.rows) }

                    // ── Note ──
                    item {
                        OutlinedTextField(
                            value = uiState.note,
                            onValueChange = viewModel::onNoteChange,
                            label = { Text("Note (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                    }

                    // ── Confirm Button ──
                    item {
                        Button(
                            onClick = viewModel::confirmSettlement,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = MaterialTheme.shapes.medium,
                            enabled = !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    "Confirm & Save Settlement",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

// ── Per-investor settlement card ──────────────────────────────────────────

@Composable
private fun SettlementRowCard(row: InvestorSettlementRow) {
    val isOverpaid = row.balanceAmount > 0
    val isBalanced = row.balanceAmount == 0.0

    val balanceColor = when {
        isBalanced -> MaterialTheme.colorScheme.onSurfaceVariant
        isOverpaid -> MaterialTheme.colorScheme.primary    // overpaid → owed back
        else       -> MaterialTheme.colorScheme.error      // underpaid → owes
    }
    val balanceLabel = when {
        isBalanced -> "Settled"
        isOverpaid -> "Overpaid — owed back"
        else       -> "Underpaid — owes"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.investorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${String.format("%.1f", row.sharePercentage)}% share",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SettlementMetric(
                    label = "Fair Share",
                    value = "AED ${String.format("%,.2f", row.fairShareAmount)}"
                )
                SettlementMetric(
                    label = "Actual Paid",
                    value = "AED ${String.format("%,.2f", row.actualPaidAmount)}",
                    alignment = Alignment.CenterHorizontally
                )
                SettlementMetric(
                    label = "Balance",
                    value = "AED ${String.format("%,.2f", kotlin.math.abs(row.balanceAmount))}",
                    valueColor = balanceColor,
                    alignment = Alignment.End
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = balanceLabel,
                style = MaterialTheme.typography.labelSmall,
                color = balanceColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Settlement summary card ───────────────────────────────────────────────

@Composable
private fun SettlementSummaryCard(rows: List<InvestorSettlementRow>) {
    val debtor   = rows.filter { it.balanceAmount < 0 }   // underpaid — they owe
    val creditor = rows.filter { it.balanceAmount > 0 }   // overpaid  — owed back

    if (debtor.isEmpty() && creditor.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Settlement Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(8.dp))
            debtor.forEach { d ->
                creditor.forEach { c ->
                    val transferAmount = minOf(
                        kotlin.math.abs(d.balanceAmount),
                        c.balanceAmount
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• ${d.investorName} → ${c.investorName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "AED ${String.format("%,.2f", transferAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Confirm to record this settlement.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

// ── Reusable metric column ────────────────────────────────────────────────

@Composable
private fun SettlementMetric(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(horizontalAlignment = alignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
