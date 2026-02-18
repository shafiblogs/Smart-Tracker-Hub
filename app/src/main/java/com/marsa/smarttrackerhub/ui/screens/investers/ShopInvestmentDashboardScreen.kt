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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttrackerhub.domain.PhaseTransactionDetail
import com.marsa.smarttrackerhub.domain.ShopInvestorSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full investment dashboard for a shop showing:
 *  - Capital summary (total raised, % allocated, investor count)
 *  - Per-investor breakdown (share %, total paid)
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
                    Icon(Icons.Default.Calculate, contentDescription = "Year-End Settlement",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                // Assign new investor
                FloatingActionButton(
                    onClick = { onAssignInvestorClick(shopId) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Assign Investor",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                // Add transaction (primary)
                ExtendedFloatingActionButton(
                    onClick = { onAddTransactionClick(shopId) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Record Payment") },
                    containerColor = MaterialTheme.colorScheme.primary
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
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }

                    // ── Capital Summary Card ──
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Capital Overview",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    CapitalMetric(
                                        label = "Total Raised",
                                        value = "AED ${String.format("%,.0f", uiState.totalCapital)}"
                                    )
                                    CapitalMetric(
                                        label = "Allocated",
                                        value = "${String.format("%.1f", uiState.allocatedPercentage)}%",
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
                                "No investors assigned yet. Tap 👤+ to assign one.",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(uiState.investors) { investor ->
                            InvestorBreakdownCard(
                                investor = investor,
                                totalShopCapital = uiState.totalCapital
                            )
                        }
                    }

                    // ── Phase Transactions ──
                    item {
                        HorizontalDivider(color = Color.LightGray)
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
                                "No payments recorded yet. Tap '+ Record Payment' to add one.",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        // Group by phase
                        val grouped = uiState.transactions.groupBy { it.phase }
                        grouped.forEach { (phase, txList) ->
                            item {
                                PhaseGroup(phase = phase, transactions = txList)
                            }
                        }
                    }

                    item { Spacer(Modifier.height(120.dp)) }
                }
            }
        }
    }
}

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

@Composable
private fun InvestorBreakdownCard(
    investor: ShopInvestorSummary,
    totalShopCapital: Double
) {
    val fairShare = if (totalShopCapital > 0)
        (investor.sharePercentage / 100.0) * totalShopCapital else 0.0
    val balance = investor.totalPaid - fairShare

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Share: ${String.format("%.1f", investor.sharePercentage)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                // Status chip
                Surface(
                    color = if (investor.status == "Active")
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = investor.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (investor.status == "Active")
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InvestorMetric("Paid", "AED ${String.format("%,.0f", investor.totalPaid)}")
                InvestorMetric(
                    "Fair Share",
                    "AED ${String.format("%,.0f", fairShare)}",
                    alignment = Alignment.CenterHorizontally
                )
                InvestorMetric(
                    label = if (balance >= 0) "Overpaid" else "Underpaid",
                    value = "AED ${String.format("%,.0f", Math.abs(balance))}",
                    valueColor = if (balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    alignment = Alignment.End
                )
            }
        }
    }
}

@Composable
private fun InvestorMetric(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = Color.Unspecified,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(horizontalAlignment = alignment) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun PhaseGroup(phase: String, transactions: List<PhaseTransactionDetail>) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    val phaseTotal = transactions.sumOf { it.amount }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

            transactions.forEach { tx ->
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tx.investorName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = dateFormat.format(Date(tx.transactionDate)),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        if (tx.note.isNotBlank()) {
                            Text(
                                text = tx.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
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
