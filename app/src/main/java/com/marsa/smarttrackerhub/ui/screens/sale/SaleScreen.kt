package com.marsa.smarttrackerhub.ui.screens.sale

import android.app.Application
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.domain.MonthlySummary
import com.marsa.smarttrackerhub.ui.components.InfoRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleScreen(userAccessCode: AccessCode) {
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
    val selectedMonthId by viewModel.selectedMonthId.collectAsState()
    val summariesCache by viewModel.summariesCache.collectAsState()
    val expanded by viewModel.expanded.collectAsState()
    val isLoadingMonth by viewModel.isLoadingMonth.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Shop Dropdown
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

        // Display content based on state
        when {
            selectedShop == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select a shop to view monthly summaries",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            availableMonths.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No summaries available for ${selectedShop?.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                // Show list of months - clicking loads the data
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableMonths) { monthItem ->
                        val isSelected = selectedMonthId == monthItem.id
                        val summary = summariesCache[monthItem.id]

                        MonthCard(
                            monthItem = monthItem,
                            isSelected = isSelected,
                            summary = summary,
                            isLoading = isSelected && isLoadingMonth,
                            shopName = selectedShop?.name ?: "",
                            shopAddress = selectedShop?.address ?: "",
                            onClick = { viewModel.selectMonth(monthItem.id) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun MonthCard(
    monthItem: MonthItem,
    isSelected: Boolean,
    summary: MonthlySummary?,
    isLoading: Boolean,
    shopName: String,
    shopAddress: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month header - always visible
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = monthItem.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (shopAddress.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = shopAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (isSelected) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isSelected) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Expanded content - only show when selected
            if (isSelected) {
                Spacer(modifier = Modifier.height(12.dp))

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    summary != null -> {
                        // Show the full summary details
                        SummaryContent(summary = summary)
                    }

                    else -> {
                        Text(
                            text = "Failed to load data",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryContent(summary: MonthlySummary) {
    Column {
        // Balances Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Balances",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Opening",
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Closing",
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }

        Divider(Modifier.padding(vertical = 6.dp))

        BalanceComparisonRow("Cash", summary.openingCashBalance, summary.cashBalance)
        BalanceComparisonRow("Account", summary.openingAccountBalance, summary.accountBalance)
        BalanceComparisonRow("Credit", summary.openingCreditBalance, summary.creditSaleBalance)

        Divider(Modifier.padding(vertical = 8.dp))

        // Totals Section
        InfoRow(
            "💰 Average Sale",
            summary.averageSale ?: 0.0,
            color = MaterialTheme.colorScheme.primary
        )
        InfoRow("💰 Total Sale", summary.totalSales, color = MaterialTheme.colorScheme.primary)
        InfoRow("🛒 Total Purchase", summary.totalPurchases, color = MaterialTheme.colorScheme.error)
        InfoRow("💳 Total Expense", summary.totalExpenses, color = MaterialTheme.colorScheme.error)
        InfoRow("💰 Total Cash In", summary.totalCashIn, color = MaterialTheme.colorScheme.primary)
        InfoRow("🛒 Total Cash Out", summary.totalCashOut, color = MaterialTheme.colorScheme.error)
        InfoRow(
            "💳 Credit Sale Total",
            summary.totalCreditSale,
            color = MaterialTheme.colorScheme.error
        )
        InfoRow(
            "💰 Credit Sale Payment",
            summary.creditSalePayment,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun BalanceComparisonRow(label: String, opening: Double, current: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "₹%.2f".format(opening),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "₹%.2f".format(current),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = if (current < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}