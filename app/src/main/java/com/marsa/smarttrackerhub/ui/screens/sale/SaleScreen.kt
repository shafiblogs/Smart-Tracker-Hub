package com.marsa.smarttrackerhub.ui.screens.sale

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttracker.ui.theme.semanticStatusColors
import com.marsa.smarttrackerhub.domain.MonthlySummary
import com.marsa.smarttrackerhub.ui.components.MetricCell
import com.marsa.smarttrackerhub.ui.screens.chart.salesMarginColor
import com.marsa.smarttrackerhub.utils.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleScreen(
    userAccessCode: AccessCode,
    onMonthClick: (shopId: String, monthId: String, shopName: String) -> Unit
) {
    val context = LocalContext.current
    val firebaseApp = FirebaseApp.getInstance("SmartTrackerApp")
    val viewModel: SaleScreenViewModel = viewModel(
        factory = SaleScreenViewModelFactory(
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
        // Shop dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { viewModel.setExpanded(!expanded) }
        ) {
            OutlinedTextField(
                value = selectedShop?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Shop") },
                placeholder = { if (selectedShop == null) Text("Choose a shop...") },
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
            selectedShop == null -> CenterText("Select a shop to view sales")
            availableMonths.isEmpty() -> CenterText("No summaries available for ${selectedShop?.name}")
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(availableMonths) { monthItem ->
                        MonthListCard(
                            monthLabel = monthItem.displayName,
                            shopName = selectedShop?.name ?: "",
                            onClick = {
                                val sid = selectedShop?.shopId ?: return@MonthListCard
                                onMonthClick(sid, monthItem.id, selectedShop?.name ?: "")
                            },
                            details = { SaleMonthMetrics(summariesCache[monthItem.id]) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

/**
 * Per-month sale row detail — an aligned two-column grid:
 *   Sale  Đx        Purchase  Đy     (raw amounts, neutral)
 *   GP    Đz        Margin    w%     (GP colored by sign; margin by salesMarginColor)
 * Shows a placeholder while the summary is still loading.
 */
@Composable
private fun SaleMonthMetrics(summary: MonthlySummary?) {
    val colors = MaterialTheme.colorScheme
    if (summary == null) {
        Text(
            text = "—",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
        return
    }

    val totalSales = summary.totalSales
    val hasSales = totalSales > 0.0
    val grossProfit = totalSales - summary.totalPurchases
    val marginPct = if (hasSales) grossProfit / totalSales * 100 else 0.0
    val marginColor = if (hasSales) salesMarginColor(marginPct, colors.error) else colors.onSurfaceVariant
    val status = semanticStatusColors()
    val gpColor = if (grossProfit >= 0) status.success else status.danger

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCell("Sale", formatMoney(totalSales, 0), colors.onSurface)
            MetricCell("Purchase", formatMoney(summary.totalPurchases, 0), colors.onSurface)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCell("GP", formatMoney(grossProfit, 0), gpColor)
            MetricCell("Margin", if (hasSales) "${"%.0f".format(marginPct)}%" else "—", marginColor)
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
