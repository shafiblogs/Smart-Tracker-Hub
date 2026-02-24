package com.marsa.smarttrackerhub.ui.screens.investers

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttrackerhub.domain.PhaseTransactionDetail
import com.marsa.smarttrackerhub.domain.ShopInvestorSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full investment dashboard for a shop showing:
 *  - Capital summary (total raised, % allocated, investor count)
 *  - Per-investor breakdown (share %, total paid, fair share, balance)
 *  - All transactions grouped by phase
 *  - FABs: Add Transaction, Assign Investor, Year-End Settlement
 *
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun ShopInvestmentDashboardScreen(
    shopId: Int,
    onAddTransactionClick: (shopId: Int) -> Unit,
    onAssignInvestorClick: (shopId: Int) -> Unit,
    onSettlementClick: (shopId: Int) -> Unit
) {
    val viewModel: ShopInvestmentDashboardViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(shopId) {
        viewModel.init(context, shopId)
    }

    // Edit share % dialog
    uiState.editingInvestor?.let {
        EditShareDialog(
            investorName = it.investorName,
            currentShare = it.sharePercentage,
            inputValue = uiState.editShareInput,
            errorMessage = uiState.editShareError,
            isSaving = uiState.isSavingShare,
            onValueChange = viewModel::onEditShareInputChange,
            onConfirm = viewModel::saveEditedShare,
            onDismiss = viewModel::dismissEditShareDialog
        )
    }

    // Withdraw investor confirmation dialog
    uiState.withdrawingInvestor?.let { investor ->
        WithdrawInvestorDialog(
            investorName = investor.investorName,
            isWithdrawing = uiState.isWithdrawing,
            onConfirm = viewModel::confirmWithdrawInvestor,
            onDismiss = viewModel::dismissWithdrawDialog
        )
    }

    // Edit/Delete transaction dialog
    uiState.editingTransaction?.let { tx ->
        EditTransactionDialog(
            tx = tx,
            amountInput = uiState.editTxAmount,
            phaseInput = uiState.editTxPhase,
            noteInput = uiState.editTxNote,
            amountError = uiState.editTxAmountError,
            phaseError = uiState.editTxPhaseError,
            isSaving = uiState.isSavingTransaction,
            onAmountChange = viewModel::onEditTxAmountChange,
            onPhaseChange = viewModel::onEditTxPhaseChange,
            onNoteChange = viewModel::onEditTxNoteChange,
            onSave = viewModel::saveEditedTransaction,
            onDelete = viewModel::deleteTransaction,
            onDismiss = viewModel::dismissEditTransactionDialog
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Year-End Settlement
                FloatingActionButton(
                    onClick = { onSettlementClick(shopId) },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Year-End Settlement",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                // Assign new investor
                FloatingActionButton(
                    onClick = { onAssignInvestorClick(shopId) },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Assign Investor"
                    )
                }
            }
        },
        bottomBar = {
            // Record Payment — pinned to the bottom edge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                ExtendedFloatingActionButton(
                    onClick = { onAddTransactionClick(shopId) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Record Payment") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = paddingValues.calculateTopPadding() + 8.dp,
                        bottom = paddingValues.calculateBottomPadding() + 200.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // ── Capital Summary Card ──
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
                                    text = uiState.shopName.ifBlank { "Investment Dashboard" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Capital Overview",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    CapitalMetric(
                                        label = "Total Raised",
                                        value = "AED ${
                                            String.format(
                                                "%,.0f",
                                                uiState.totalCapital
                                            )
                                        }"
                                    )
                                    CapitalMetric(
                                        label = "Allocated",
                                        value = "${
                                            String.format(
                                                "%.1f",
                                                uiState.allocatedPercentage
                                            )
                                        }%",
                                        alignment = Alignment.CenterHorizontally
                                    )
                                    CapitalMetric(
                                        label = "Investors",
                                        value = uiState.investorCount.toString(),
                                        alignment = Alignment.End
                                    )
                                }
                            }
                        }
                    }

                    // ── Investor Breakdown ──
                    item {
                        Text(
                            text = "Investor Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (uiState.investors.isEmpty()) {
                        item {
                            Text(
                                "No investors assigned yet. Tap the person icon to assign one.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(uiState.investors) { investor ->
                            InvestorBreakdownCard(
                                investor = investor,
                                totalShopCapital = uiState.totalCapital,
                                onEditShareClick = { viewModel.showEditShareDialog(investor) },
                                onWithdrawClick = { viewModel.showWithdrawDialog(investor) }
                            )
                        }
                    }

                    // ── Phase Transactions ──
                    item {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Payment History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (uiState.transactions.isEmpty()) {
                        item {
                            Text(
                                "No payments recorded yet. Tap 'Record Payment' to add one.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        val grouped = uiState.transactions.groupBy { it.phase }
                        grouped.forEach { (phase, txList) ->
                            item {
                                PhaseGroup(
                                    phase = phase,
                                    transactions = txList,
                                    onEditTransaction = { viewModel.showEditTransactionDialog(it) }
                                )
                            }
                        }
                    }

                }
            }
        }
    }
}

// ── Capital metric column ─────────────────────────────────────────────────

@Composable
private fun CapitalMetric(
    label: String,
    value: String,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(horizontalAlignment = alignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// ── Per-investor breakdown card ────────────────────────────────────────────

@Composable
private fun InvestorBreakdownCard(
    investor: ShopInvestorSummary,
    totalShopCapital: Double,
    onEditShareClick: () -> Unit,
    onWithdrawClick: () -> Unit
) {
    val fairShare = if (totalShopCapital > 0)
        (investor.sharePercentage / 100.0) * totalShopCapital else 0.0
    val balance = investor.totalPaid - fairShare
    val isActive = investor.status == "Active"

    val balanceColor = when {
        balance >= 0 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = investor.investorName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Share: ${String.format("%.1f", investor.sharePercentage)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Status chip
                    Surface(
                        color = if (isActive)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = investor.status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    // Edit share % button
                    IconButton(
                        onClick = onEditShareClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit share %",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Withdraw button — only shown for active investors
                    if (isActive) {
                        IconButton(
                            onClick = onWithdrawClick,
                            modifier = Modifier.size(36.dp)   // 36 dp = M3 minimum touch target
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ExitToApp,
                                contentDescription = "Withdraw investor",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InvestorMetric(
                    "Paid",
                    "AED ${String.format("%,.0f", investor.totalPaid)}"
                )
                InvestorMetric(
                    "Fair Share",
                    "AED ${String.format("%,.0f", fairShare)}",
                    alignment = Alignment.CenterHorizontally
                )
                InvestorMetric(
                    label = if (balance >= 0) "Overpaid" else "Underpaid",
                    value = "AED ${String.format("%,.0f", kotlin.math.abs(balance))}",
                    valueColor = balanceColor,
                    alignment = Alignment.End
                )
            }
        }
    }
}

// ── Investor metric column ────────────────────────────────────────────────

@Composable
private fun InvestorMetric(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
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

// ── Edit Share % Dialog ───────────────────────────────────────────────────

@Composable
private fun EditShareDialog(
    investorName: String,
    currentShare: Double,
    inputValue: String,
    errorMessage: String?,
    isSaving: Boolean,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = "Edit Share — $investorName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Current share: ${String.format("%.2f", currentShare)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "All investors' shares must add up to 100%. Edit each investor individually to reach the correct split.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = onValueChange,
                    label = { Text("New Share %") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}

// ── Edit / Delete Transaction Dialog ─────────────────────────────────────

@Composable
private fun EditTransactionDialog(
    tx: PhaseTransactionDetail,
    amountInput: String,
    phaseInput: String,
    noteInput: String,
    amountError: String?,
    phaseError: String?,
    isSaving: Boolean,
    onAmountChange: (String) -> Unit,
    onPhaseChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = "Edit Payment — ${tx.investorName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = onAmountChange,
                    label = { Text("Amount (AED)") },
                    prefix = { Text("AED ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError != null,
                    supportingText = amountError?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phaseInput,
                    onValueChange = onPhaseChange,
                    label = { Text("Phase") },
                    singleLine = true,
                    isError = phaseError != null,
                    supportingText = phaseError?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = onNoteChange,
                    label = { Text("Note (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                // Delete button inside dialog
                TextButton(
                    onClick = onDelete,
                    enabled = !isSaving,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete this payment")
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}

// ── Withdraw Investor Confirmation Dialog ─────────────────────────────────

@Composable
private fun WithdrawInvestorDialog(
    investorName: String,
    isWithdrawing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isWithdrawing) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Withdraw Investor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Mark $investorName as Withdrawn from this shop? " +
                        "Their past transactions will be preserved but they won't be " +
                        "included in future settlements or share allocations.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isWithdrawing,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isWithdrawing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                } else {
                    Text("Withdraw")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWithdrawing) {
                Text("Cancel")
            }
        }
    )
}

// ── Phase group card ──────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhaseGroup(
    phase: String,
    transactions: List<PhaseTransactionDetail>,
    onEditTransaction: (PhaseTransactionDetail) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    val phaseTotal = transactions.sumOf { it.amount }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Phase header
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
                Text(
                    text = "AED ${String.format("%,.0f", phaseTotal)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
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
                            onLongClick = { onEditTransaction(tx) }
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
                        Text(
                            text = "Hold to edit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                    }
                    Text(
                        text = "AED ${String.format("%,.0f", tx.amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
