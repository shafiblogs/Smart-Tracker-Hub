package com.marsa.smarttrackerhub.ui.screens.investers

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttrackerhub.domain.InvestorSettlementRow

/**
 * Year-End Settlement Calculator for a specific shop.
 *
 * Shows:
 *  - Total invested in the shop
 *  - Per-investor breakdown: fair share vs actual paid vs balance
 *  - Year field + optional note
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

    LaunchedEffect(shopId) {
        viewModel.init(context, shopId)
    }

    // Navigate back once saved
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onSettlementSaved()
    }

    // Show errors in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

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
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No investment data found.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Text(
                            "Record transactions first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
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

                    // ── Total Capital Card ──
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
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
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Total Invested",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "AED ${String.format("%,.2f", uiState.totalInvested)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // ── Settlement Year ──
                    item {
                        OutlinedTextField(
                            value = uiState.year.toString(),
                            onValueChange = { v -> v.toIntOrNull()?.let { viewModel.onYearChange(it) } },
                            label = { Text("Settlement Year") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
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

                    items(uiState.rows) { row ->
                        SettlementRowCard(row = row)
                    }

                    // ── Who owes whom summary ──
                    item {
                        SettlementSummaryCard(rows = uiState.rows)
                    }

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
                            modifier = Modifier.fillMaxWidth(),
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
                                Text("Confirm & Save Settlement")
                            }
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

// ── Per-investor settlement card ─────────────────────────────────────────

@Composable
private fun SettlementRowCard(row: InvestorSettlementRow) {
    val isOverpaid = row.balanceAmount > 0
    val isBalanced = row.balanceAmount == 0.0

    val balanceColor = when {
        isBalanced -> Color.Gray
        isOverpaid -> Color(0xFF2E7D32)  // green — overpaid, owed back
        else -> MaterialTheme.colorScheme.error  // red — underpaid, owes
    }
    val balanceLabel = when {
        isBalanced -> "Settled"
        isOverpaid -> "Overpaid — owed back"
        else -> "Underpaid — owes"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format("%.1f", row.sharePercentage)}% share",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
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

// ── Settlement summary: who pays whom ────────────────────────────────────

@Composable
private fun SettlementSummaryCard(rows: List<InvestorSettlementRow>) {
    val debtor = rows.filter { it.balanceAmount < 0 }   // underpaid — they owe
    val creditor = rows.filter { it.balanceAmount > 0 } // overpaid  — owed back

    if (debtor.isEmpty() && creditor.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                    Text(
                        text = "• ${d.investorName} owes ${c.investorName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
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
            color = Color.Gray
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            fontSize = 13.sp
        )
    }
}
