package com.marsa.smarttrackerhub.ui.screens.summary

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.google.firebase.FirebaseApp
import com.marsa.smarttracker.ui.theme.semanticStatusColors
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.domain.AccountSummary
import com.marsa.smarttrackerhub.ui.components.AedText
import com.marsa.smarttrackerhub.ui.screens.sale.MonthListCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    userAccessCode: AccessCode,
    onMonthClick: (shopId: String, monthId: String, shopName: String) -> Unit
) {
    val context = LocalContext.current
    val firebaseApp = FirebaseApp.getInstance("AccountTrackerApp")
    val viewModel: SummaryViewModel = viewModel(
        factory = SummaryScreenViewModelFactory(
            context.applicationContext as Application,
            firebaseApp
        )
    )

    LaunchedEffect(userAccessCode) {
        viewModel.loadScreenData(userAccessCode)
    }

    val shops by viewModel.shops.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val selectedShop by viewModel.selectedShop.collectAsState()
    val expanded by viewModel.expanded.collectAsState()
    val summariesCache by viewModel.summariesCache.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Region dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { viewModel.setExpanded(!expanded) }
        ) {
            OutlinedTextField(
                value = selectedShop?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Region") },
                placeholder = { if (selectedShop == null) Text("Choose a region...") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { viewModel.setExpanded(false) },
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                shops.forEach { shop ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(shop.name ?: "-", style = MaterialTheme.typography.bodyLarge)
                                if (!shop.address.isNullOrBlank()) {
                                    Text(
                                        text = shop.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onClick = {
                            viewModel.setSelectedShop(shop)
                            viewModel.setExpanded(false)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            selectedShop == null -> CenterText("Select a region to view monthly summaries")
            availableMonths.isEmpty() -> CenterText("No summaries available for ${selectedShop?.name}")
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(availableMonths) { monthItem ->
                        val summary = summariesCache[monthItem.id]
                        val status = semanticStatusColors()
                        MonthListCard(
                            monthLabel = monthItem.displayName,
                            shopName = selectedShop?.name ?: "",
                            onClick = {
                                val sid = selectedShop?.shopId ?: return@MonthListCard
                                onMonthClick(sid, monthItem.id, selectedShop?.name ?: "")
                            },
                            lastUpdated = summary?.lastUpdated,
                            headline = if (summary != null) ({
                                val netColor = if (summary.netProfit >= 0) status.success else status.danger
                                val sign = if (summary.netProfit >= 0) "+" else ""
                                Column(horizontalAlignment = Alignment.End) {
                                    AedText(
                                        amount = summary.netProfit,
                                        prefix = sign,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = netColor
                                    )
                                    Text(
                                        text = "net profit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = netColor
                                    )
                                }
                            }) else null,
                            underHeader = if (summary != null) ({
                                AccountMonthCashFlowBars(summary)
                            }) else null,
                            details = { AccountMonthSecondaryMetrics(summary) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

/**
 * Cash flow bars that render under the month headline (in underHeader slot).
 * Shows In/Out stacked bars and retained amount to indicate the month's cash position.
 */
@Composable
private fun AccountMonthCashFlowBars(summary: AccountSummary?) {
    val colors = MaterialTheme.colorScheme
    if (summary == null) return

    val status = semanticStatusColors()
    val cashIn = summary.totalCollection
    val cashOut = summary.openingCashBalance + summary.totalCollection - summary.cashBalance
    val maxCash = kotlin.math.max(cashIn, cashOut).coerceAtLeast(1.0)
    val gpColor = if (summary.grossProfit >= 0) status.success else status.danger
    val retained = cashIn - cashOut

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // In bar with label above
        Text(
            text = "In",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 32.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.outlineVariant.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((cashIn / maxCash).toFloat().coerceIn(0.01f, 1f))
                    .fillMaxHeight()
                    .background(colors.primary)
            )
        }
        AedText(
            amount = cashIn,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )

        // Out bar with label above
        Text(
            text = "Out",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 32.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.outlineVariant.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((cashOut / maxCash).toFloat().coerceIn(0.01f, 1f))
                    .fillMaxHeight()
                    .background(colors.outline)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AedText(
                amount = cashOut,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            AedText(
                amount = retained,
                prefix = if (retained >= 0) "+" else "",
                suffix = " retained",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (retained >= 0) gpColor else status.danger
            )
        }
    }
}

/**
 * Secondary metrics for the Account month card detail section.
 * Shows Gross Profit, Expense, and Withdrawal in three-column label-over-value layout.
 */
@Composable
private fun AccountMonthSecondaryMetrics(summary: AccountSummary?) {
    val colors = MaterialTheme.colorScheme
    if (summary == null) {
        Text("—", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        return
    }
    val status = semanticStatusColors()
    val gpColor = if (summary.grossProfit >= 0) status.success else status.danger

    // Three-column label-over-value layout, matching SaleMonthMetrics' primary row
    // so the Sales and Account cards read identically when switching tabs.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Gross profit",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            AedText(
                amount = summary.grossProfit,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = gpColor
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Expense",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            AedText(
                amount = summary.totalExpenses,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Withdrawal",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            AedText(
                amount = summary.withdrawal,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface
            )
        }
    }
}

@Composable
private fun CenterText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
