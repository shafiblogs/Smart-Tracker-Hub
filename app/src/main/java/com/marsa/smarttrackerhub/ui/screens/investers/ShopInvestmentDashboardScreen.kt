package com.marsa.smarttrackerhub.ui.screens.investers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttrackerhub.ui.components.DetailSectionCard
import com.marsa.smarttrackerhub.ui.components.MetricCell

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
    isAdmin: Boolean = false,
    onAddTransactionClick: (shopId: Int) -> Unit,
    onAssignInvestorClick: (shopId: Int) -> Unit,
    onSettlementClick: (shopId: Int) -> Unit
) {
    val viewModel: ShopInvestmentDashboardViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isClosed = uiState.shopStatus == "Closed"

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
            if (!isClosed && isAdmin) {
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
            }
        },
        bottomBar = {
            if (!isClosed && isAdmin) {
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
                    contentPadding = PaddingValues(
                        top = paddingValues.calculateTopPadding() + 8.dp,
                        bottom = paddingValues.calculateBottomPadding() + 200.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // ── Capital Summary Card ──
                    item {
                        DetailSectionCard(
                            title = "Capital",
                            subtitle = uiState.shopName.ifBlank { "Investment Dashboard" }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    MetricCell("Total Raised", uiState.totalCapital, valueColor = MaterialTheme.colorScheme.primary)
                                    MetricCell("Allocated", "${"%.0f".format(uiState.allocatedPercentage)}%", MaterialTheme.colorScheme.onSurface)
                                }
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    MetricCell("Investors", uiState.investorCount.toString(), MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // ── Closed Shop Banner ──
                    if (isClosed) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Shop is Closed",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = "New investments and payments cannot be recorded for a closed shop.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Investor Breakdown ──
                    item {
                        InvestorBreakdownSection(
                            investors = uiState.investors,
                            totalShopCapital = uiState.totalCapital,
                            isAdmin = isAdmin,
                            onEditShareClick = { viewModel.showEditShareDialog(it) },
                            onWithdrawClick = { viewModel.showWithdrawDialog(it) }
                        )
                    }

                    // ── Phase Transactions ──
                    item {
                        PaymentHistorySection(
                            transactions = uiState.transactions,
                            isAdmin = isAdmin,
                            onEditTransaction = { viewModel.showEditTransactionDialog(it) }
                        )
                    }
                }
            }
        }
    }
}
