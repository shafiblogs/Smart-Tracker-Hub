package com.marsa.smarttrackerhub.ui.screens.summary

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.marsa.smarttrackerhub.ui.components.MetricCell
import com.marsa.smarttrackerhub.ui.screens.sale.MonthListCard
import com.marsa.smarttrackerhub.utils.formatMoney

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
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (summary.netProfit >= 0) "+" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (summary.netProfit >= 0) status.success else status.danger
                                    )
                                    Text(
                                        text = formatMoney(summary.netProfit, 0),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (summary.netProfit >= 0) status.success else status.danger
                                    )
                                    Text(
                                        text = "net profit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (summary.netProfit >= 0) status.success else status.danger
                                    )
                                }
                            }) else null,
                            details = { AccountMonthMetrics(summary) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

/**
 * Per-month account row detail — headline shows net profit, this shows cash flow stacked bars.
 * In/Out bars show the gap between money in and money out.
 * Underneath: GP, Expense, Withdrawal (neutral).
 */
@Composable
private fun AccountMonthMetrics(summary: AccountSummary?) {
    val colors = MaterialTheme.colorScheme
    if (summary == null) {
        Text("—", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        return
    }
    val status = semanticStatusColors()
    val cashIn = summary.totalCollection
    val cashOut = summary.openingCashBalance + summary.totalCollection - summary.cashBalance
    val maxCash = kotlin.math.max(cashIn, cashOut).coerceAtLeast(1.0)
    val gpColor = if (summary.grossProfit >= 0) status.success else status.danger
    val retained = cashIn - cashOut

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // In bar
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "In",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.width(40.dp)
            )
            Box(
                modifier = Modifier
                    .weight((cashIn / maxCash).toFloat().coerceIn(0.01f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.primary)
            )
        }
        Text(
            text = formatMoney(cashIn, 0),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = 40.dp)
        )

        // Out bar
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Out",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.width(40.dp)
            )
            Box(
                modifier = Modifier
                    .weight((cashOut / maxCash).toFloat().coerceIn(0.01f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.outline)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMoney(cashOut, 0),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.width(40.dp)
            )
            Text(
                text = "${if (retained >= 0) "+" else ""}${formatMoney(retained, 0)} retained",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (retained >= 0) gpColor else colors.error
            )
        }

        // Divider
        androidx.compose.material3.HorizontalDivider(color = colors.outlineVariant)

        // Secondary metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Gross profit",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Text(
                text = formatMoney(summary.grossProfit, 0),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = gpColor
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Expense",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Text(
                text = formatMoney(summary.totalExpenses, 0),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurface
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Withdrawal",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Text(
                text = formatMoney(summary.withdrawal, 0),
                style = MaterialTheme.typography.bodySmall,
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
