package com.marsa.smarttrackerhub.ui.screens.home

import android.app.Application
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.ui.screens.chart.MonthlySalesChart
import com.marsa.smarttrackerhub.ui.screens.chart.StatisticsCard
import com.marsa.smarttrackerhub.utils.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userAccessCode: AccessCode
) {
    val context = LocalContext.current
    val viewModel: HomeScreenViewModel = viewModel(
        factory = HomeScreenViewModelFactory(context.applicationContext as Application)
    )
    val selectedShop by viewModel.selectedShop.collectAsState()
    val chartData by viewModel.chartData.collectAsState()
    val shops by viewModel.shops.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val expanded by viewModel.expanded.collectAsState()
    val periodLabel by viewModel.periodLabel.collectAsState()

    // Period selection states
    val availableRanges by viewModel.availableRanges.collectAsState()
    val selectedRange by viewModel.selectedRange.collectAsState()
    val periodExpanded by viewModel.periodExpanded.collectAsState()

    var chartView by remember { mutableStateOf<View?>(null) }
    var statsView by remember { mutableStateOf<View?>(null) }

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

        Spacer(modifier = Modifier.height(12.dp))

        // Period Selector (NEW)
        ExposedDropdownMenuBox(
            expanded = periodExpanded,
            onExpandedChange = { viewModel.setPeriodExpanded(!periodExpanded) }
        ) {
            OutlinedTextField(
                value = selectedRange.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Time Period") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(periodExpanded) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = periodExpanded,
                onDismissRequest = { viewModel.setPeriodExpanded(false) }
            ) {
                availableRanges.forEach { range ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = range.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            viewModel.setSelectedRange(range)
                            viewModel.setPeriodExpanded(false)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Statistics Card
        statistics?.let { stats ->
            AndroidView(
                factory = { ctx ->
                    androidx.compose.ui.platform.ComposeView(ctx).apply {
                        setContent {
                            StatisticsCard(
                                statistics = stats,
                                shopAddress = selectedShop?.address ?: "",
                                periodLabel = periodLabel,
                                onShareClick = {
                                    statsView?.let { view ->
                                        ShareUtil.shareViewAsImage(
                                            view = view,
                                            context = context,
                                            fileName = "sales_stats_${selectedShop?.name?.replace(" ", "_")}.png",
                                            shareTitle = "Share Sales Statistics"
                                        )
                                    }
                                }
                            )
                        }
                    }.also { composeView ->
                        statsView = composeView
                    }
                },
                update = { view ->
                    view.setContent {
                        StatisticsCard(
                            statistics = stats,
                            shopAddress = selectedShop?.address ?: "",
                            periodLabel = periodLabel,
                            onShareClick = {
                                statsView?.let { v ->
                                    ShareUtil.shareViewAsImage(
                                        view = v,
                                        context = context,
                                        fileName = "sales_stats_${selectedShop?.name?.replace(" ", "_")}.png",
                                        shareTitle = "Share Sales Statistics"
                                    )
                                }
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Chart Title with Share Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sales Trends",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            IconButton(
                onClick = {
                    chartView?.let { view ->
                        ShareUtil.shareViewAsImage(
                            view = view,
                            context = context,
                            fileName = "sales_chart_${selectedShop?.name?.replace(" ", "_")}.png",
                            shareTitle = "Share Sales Chart"
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                MonthlySalesChart(
                                    data = chartData,
                                    shopAddress = selectedShop?.address ?: "",
                                    periodLabel = periodLabel,
                                    isTargetAchieved = (statistics?.averageAchievementPercentage ?: 0.0) >= 100,
                                    achievementPercentage = statistics?.averageAchievementPercentage ?: 0.0, // ADD this
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
                update = { view ->
                    view.setContent {
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            MonthlySalesChart(
                                data = chartData,
                                shopAddress = selectedShop?.address ?: "",
                                periodLabel = periodLabel,
                                isTargetAchieved = (statistics?.averageAchievementPercentage ?: 0.0) >= 100,
                                achievementPercentage = statistics?.averageAchievementPercentage ?: 0.0, // ADD this
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(350.dp)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}