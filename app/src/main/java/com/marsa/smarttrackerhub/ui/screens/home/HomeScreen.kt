package com.marsa.smarttrackerhub.ui.screens.home

import android.app.Application
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttrackerhub.domain.AccessCode

/**
 * Home screen with sales chart
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userAccessCode: AccessCode
) {
    val context = LocalContext.current
    val viewModel: SalesChartViewModel = viewModel(
        factory = SalesChartViewModelFactory(context.applicationContext as Application)
    )
    val selectedShop by viewModel.selectedShop.collectAsState()
    val chartData by viewModel.chartData.collectAsState()
    val shops by viewModel.shops.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val expanded by viewModel.expanded.collectAsState()


    LaunchedEffect(Unit) {
        viewModel.loadScreenData(userAccessCode)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // Shop Selector
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

        Spacer(modifier = Modifier.height(16.dp))

        // Statistics Card
        statistics?.let { stats ->
            StatisticsCard(statistics = stats, selectedShop?.address ?: "")
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Chart Title
        Text(
            text = "4 Months Trend",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (chartData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available for selected shop",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                MonthlySalesChart(
                    data = chartData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chart Help Text
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tap on any point to see detailed information",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Shop selector dropdown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShopSelector(
    selectedShopId: String?,
    shops: List<ShopInfo>,
    onShopSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedShop = shops.find { it.shopId == selectedShopId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedShop?.displayName ?: "Select Shop",
            onValueChange = {},
            readOnly = true,
            label = { Text("Shop") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            shops.forEach { shop ->
                DropdownMenuItem(
                    text = { Text(shop.displayName) },
                    onClick = {
                        onShopSelected(shop.shopId)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Statistics summary card
 */
@Composable
private fun StatisticsCard(
    statistics: ChartStatistics,
    shopAddress: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "$shopAddress (${statistics.totalMonths} Months)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // First Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Avg Target",
                    value = String.format("%.1f", statistics.totalTarget / statistics.totalMonths),
                    color = Color.Gray
                )

                StatItem(
                    label = "Avg Sale",
                    value = String.format("%.1f", statistics.totalAverage / statistics.totalMonths),
                    color = if (statistics.averageAchievementPercentage >= 100)
                        Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Second Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Achievement",
                    value = String.format("%.1f%%", statistics.averageAchievementPercentage),
                    color = if (statistics.averageAchievementPercentage >= 100)
                        Color(0xFF4CAF50) else Color(0xFFF44336)
                )

                StatItem(
                    label = "Targets Met",
                    value = "${statistics.monthsTargetMet}/${statistics.totalMonths}",
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Individual stat item
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
