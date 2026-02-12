package com.marsa.smarttrackerhub.ui.screens.home

import android.app.Application
import android.view.View
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.viewinterop.AndroidView
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

    // Reference to the chart card for screenshot
    var chartView by remember { mutableStateOf<View?>(null) }

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

        Spacer(modifier = Modifier.height(20.dp))

        // Statistics Card (Last 3 Months Only)
        statistics?.let { stats ->
            StatisticsCard(statistics = stats, selectedShop?.address ?: "")
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Chart Title with Share Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = " Sales Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Share button
            IconButton(
                onClick = {
                    chartView?.let { view ->
                        ChartShareUtil.shareChartImage(
                            view = view,
                            context = context,
                            fileName = "sales_chart_${selectedShop?.name?.replace(" ", "_")}.png"
                        )
                    }
                },
                enabled = chartData.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Chart",
                    tint = if (chartData.isNotEmpty())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chart Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            // Wrap in AndroidView to get View reference for screenshot
            AndroidView(
                factory = { ctx ->
                    androidx.compose.ui.platform.ComposeView(ctx).apply {
                        setContent {
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
                    }.also { composeView ->
                        chartView = composeView
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Statistics summary card
 * Shows last 3 months only (excluding current month)
 */
@Composable
private fun StatisticsCard(
    statistics: ChartStatistics,
    shopAddress: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = "$shopAddress (${statistics.totalMonths} Months)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Avg Target",
                    value = String.format("%.1f", statistics.totalTarget / statistics.totalMonths),
                    valueColor = colors.onSecondaryContainer
                )

                StatItem(
                    label = "Avg Sale",
                    value = String.format("%.1f", statistics.totalAverage / statistics.totalMonths),
                    valueColor = if (statistics.averageAchievementPercentage >= 100)
                        colors.tertiary
                    else
                        colors.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Achievement",
                    value = String.format("%.1f%%", statistics.averageAchievementPercentage),
                    valueColor = if (statistics.averageAchievementPercentage >= 100)
                        colors.tertiary
                    else
                        colors.error
                )

                StatItem(
                    label = "Targets",
                    value = "${statistics.monthsTargetMet}/${statistics.totalMonths}",
                    valueColor = colors.onSecondaryContainer
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
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Column(modifier = modifier) {

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

