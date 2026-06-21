package com.marsa.smarttrackerhub.ui.screens.home

import android.app.Application
import android.view.View
import androidx.compose.foundation.background
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
import com.google.firebase.FirebaseApp
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.ui.screens.chart.MonthlySalesChart
import com.marsa.smarttrackerhub.ui.screens.chart.PurchaseCategoryChart
import com.marsa.smarttrackerhub.ui.screens.chart.UnifiedStatisticsCard
import com.marsa.smarttracker.ui.theme.SmartTrackerTheme
import com.marsa.smarttrackerhub.utils.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userAccessCode: AccessCode
) {
    val context = LocalContext.current
    val firebaseApp = FirebaseApp.getInstance("SmartTrackerApp")
    val viewModel: HomeScreenViewModel = viewModel(
        factory = HomeScreenViewModelFactory(
            application = context.applicationContext as Application,
            firebaseApp = firebaseApp
        )
    )
    val selectedShop     by viewModel.selectedShop.collectAsState()
    val chartData        by viewModel.chartData.collectAsState()
    val shops            by viewModel.shops.collectAsState()
    val isLoading        by viewModel.isLoading.collectAsState()
    val statistics       by viewModel.statistics.collectAsState()
    val expanded         by viewModel.expanded.collectAsState()
    val periodLabel      by viewModel.periodLabel.collectAsState()
    val availableRanges  by viewModel.availableRanges.collectAsState()
    val selectedRange    by viewModel.selectedRange.collectAsState()
    val periodExpanded   by viewModel.periodExpanded.collectAsState()
    val salesMargin      by viewModel.salesMargin.collectAsState()

    // Purchase chart state
    val isPurchaseLoading    by viewModel.isPurchaseLoading.collectAsState()
    val purchaseCategoryData by viewModel.purchaseCategoryData.collectAsState()
    val purchaseStatistics   by viewModel.purchaseStatistics.collectAsState()

    var chartView              by remember { mutableStateOf<View?>(null) }
    var statsView              by remember { mutableStateOf<View?>(null) }
    var purchaseChartView      by remember { mutableStateOf<View?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadScreenData(userAccessCode)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // ── Shop Selector ────────────────────────────────────────────────────
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

        // ── Period Selector ──────────────────────────────────────────────────
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

        // ── Unified Statistics Card (Sales + Purchases) ──────────────────────
        if (statistics != null && purchaseStatistics != null) {
            AndroidView(
                factory = { ctx ->
                    androidx.compose.ui.platform.ComposeView(ctx).apply {
                        setContent {
                            UnifiedStatisticsCard(
                                salesStatistics = statistics!!,
                                purchaseStatistics = purchaseStatistics!!,
                                shopName = selectedShop?.name ?: "",
                                periodLabel = periodLabel,
                                salesMargin = salesMargin,
                                onShareClick = {
                                    statsView?.let { view ->
                                        ShareUtil.shareViewAsImage(
                                            view = view,
                                            context = context,
                                            fileName = "statistics_${selectedShop?.name?.replace(" ", "_")}.png",
                                            shareTitle = "Share Statistics"
                                        )
                                    }
                                }
                            )
                        }
                    }.also { statsView = it }
                },
                update = { view ->
                    view.setContent {
                        UnifiedStatisticsCard(
                            salesStatistics = statistics!!,
                            purchaseStatistics = purchaseStatistics!!,
                            shopName = selectedShop?.name ?: "",
                            periodLabel = periodLabel,
                            salesMargin = salesMargin,
                            onShareClick = {
                                statsView?.let { v ->
                                    ShareUtil.shareViewAsImage(
                                        view = v,
                                        context = context,
                                        fileName = "statistics_${selectedShop?.name?.replace(" ", "_")}.png",
                                        shareTitle = "Share Statistics"
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

        // ── Sales Trends title + share ───────────────────────────────────────
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

        // ── Sales Chart Card ─────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    androidx.compose.ui.platform.ComposeView(ctx).apply {
                        setContent {
                            SalesChartContent(
                                isLoading = isLoading,
                                chartData = chartData,
                                selectedShop = selectedShop,
                                periodLabel = periodLabel,
                                statistics = statistics
                            )
                        }
                    }.also { chartView = it }
                },
                update = { view ->
                    view.setContent {
                        SalesChartContent(
                            isLoading = isLoading,
                            chartData = chartData,
                            selectedShop = selectedShop,
                            periodLabel = periodLabel,
                            statistics = statistics
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Purchase Trend title + share ─────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Purchase Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(
                onClick = {
                    purchaseChartView?.let { view ->
                        ShareUtil.shareViewAsImage(
                            view = view,
                            context = context,
                            fileName = "purchase_chart_${selectedShop?.name?.replace(" ", "_")}.png",
                            shareTitle = "Share Purchase Trend"
                        )
                    }
                },
                enabled = purchaseCategoryData.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Purchase Chart",
                    tint = if (purchaseCategoryData.isNotEmpty())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Purchase Chart Card ──────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    androidx.compose.ui.platform.ComposeView(ctx).apply {
                        setContent {
                            PurchaseChartContent(
                                isPurchaseLoading    = isPurchaseLoading,
                                purchaseCategoryData = purchaseCategoryData,
                                purchaseStatistics   = purchaseStatistics,
                                selectedShop         = selectedShop,
                                periodLabel          = periodLabel
                            )
                        }
                    }.also { purchaseChartView = it }
                },
                update = { view ->
                    view.setContent {
                        PurchaseChartContent(
                            isPurchaseLoading    = isPurchaseLoading,
                            purchaseCategoryData = purchaseCategoryData,
                            purchaseStatistics   = purchaseStatistics,
                            selectedShop         = selectedShop,
                            periodLabel          = periodLabel
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Private helper composables ───────────────────────────────────────────────

@Composable
private fun PurchaseChartContent(
    isPurchaseLoading: Boolean,
    purchaseCategoryData: List<com.marsa.smarttrackerhub.ui.screens.chart.PurchaseCategoryChartData>,
    purchaseStatistics: com.marsa.smarttrackerhub.ui.screens.chart.PurchaseChartStatistics?,
    selectedShop: com.marsa.smarttrackerhub.ui.screens.statement.ShopListDto?,
    periodLabel: String = ""
) {
    // Wrap in the app theme + a solid surface background so the shared image (captured from
    // this ComposeView) isn't transparent — a transparent PNG renders dark and hides the
    // dark shop-name text, which looked like "dark mode" in light mode.
    SmartTrackerTheme {
    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
    when {
        isPurchaseLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        purchaseCategoryData.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedShop == null)
                        "Select a shop to view purchases"
                    else
                        "No purchase data available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        else -> {
            purchaseStatistics?.let { stats ->
                com.marsa.smarttrackerhub.ui.screens.chart.PurchaseCategoryChart(
                    categories   = purchaseCategoryData,
                    statistics   = stats,
                    shopAddress  = selectedShop?.name ?: "",
                    periodLabel  = periodLabel,
                    modifier     = Modifier.fillMaxWidth()
                )
            }
        }
    }
    }
    }
}

@Composable
private fun SalesChartContent(
    isLoading: Boolean,
    chartData: List<com.marsa.smarttrackerhub.ui.screens.chart.MonthlyChartData>,
    selectedShop: com.marsa.smarttrackerhub.ui.screens.statement.ShopListDto?,
    periodLabel: String,
    statistics: com.marsa.smarttrackerhub.domain.ChartStatistics?
) {
    // Themed + solid surface background so the shared capture isn't transparent (see PurchaseChartContent).
    SmartTrackerTheme {
    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
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
            shopAddress = selectedShop?.name ?: "",
            periodLabel = periodLabel,
            isTargetAchieved = (statistics?.averageAchievementPercentage ?: 0.0) >= 100,
            achievementPercentage = statistics?.averageAchievementPercentage ?: 0.0,
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
        )
    }
    }
    }
}
