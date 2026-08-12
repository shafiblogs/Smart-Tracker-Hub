package com.marsa.smarttrackerhub.ui.screens.sale

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.marsa.smarttrackerhub.domain.MonthlySummary
import com.marsa.smarttrackerhub.ui.components.AedText
import com.marsa.smarttrackerhub.ui.components.DeltaChip
import com.marsa.smarttrackerhub.ui.components.DetailSectionCard
import com.marsa.smarttrackerhub.ui.components.DropdownField
import com.marsa.smarttrackerhub.ui.components.MarginBar
import com.marsa.smarttrackerhub.ui.screens.chart.PurchaseCategoryChartData
import com.marsa.smarttrackerhub.ui.screens.chart.PurchaseChartStatistics
import com.marsa.smarttrackerhub.ui.screens.chart.salesMarginColor
import com.marsa.smarttrackerhub.ui.screens.purchase.PurchaseBreakdownSection
import com.marsa.smarttrackerhub.utils.formatLastUpdated
import com.marsa.smarttrackerhub.utils.shareCard
import com.marsa.smarttracker.ui.theme.navBarInset
import com.marsa.smarttracker.ui.theme.semanticStatusColors
import com.marsa.smarttracker.ui.theme.spaceLg
import com.marsa.smarttracker.ui.theme.spaceMd
import com.marsa.smarttracker.ui.theme.spaceSm
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesDetailScreen(
    shopId: String,
    monthId: String,
    shopName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val firebaseApp = FirebaseApp.getInstance("SmartTrackerApp")
    val viewModel: SalesDetailViewModel = viewModel(
        factory = SalesDetailViewModelFactory(
            context.applicationContext as Application, firebaseApp, shopId, monthId
        )
    )

    val availableMonths by viewModel.availableMonths.collectAsState()
    val selectedMonthId by viewModel.selectedMonthId.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val purchaseChart by viewModel.purchaseChart.collectAsState()
    val purchaseStats by viewModel.purchaseStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val comparison by viewModel.comparison.collectAsState()

    val density = LocalDensity.current
    val widthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }

    Scaffold(
        // The NavHost's outer Scaffold already offsets content below the status bar, so
        // this inner Scaffold/TopAppBar must NOT add the inset again (avoids a tall app bar
        // with empty space on top).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = shopName.ifBlank { "Sales" },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Common month selector (matches the shop/region dropdown style).
            DropdownField(
                label = "Month",
                selectedValue = selectedMonthId,
                options = availableMonths.map { it.displayName },
                onOptionSelected = { name ->
                    availableMonths.firstOrNull { it.displayName == name }
                        ?.let { viewModel.selectMonth(it.id) }
                },
                enabled = availableMonths.isNotEmpty(),
                modifier = Modifier.padding(horizontal = spaceLg, vertical = spaceSm)
            )

            Spacer(modifier = Modifier.height(spaceMd))

            when {
                isLoading && summary == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                summary != null -> {
                    val currentSummary = summary!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = spaceLg),
                        verticalArrangement = Arrangement.spacedBy(spaceLg)
                    ) {
                        MonthVerdictStrip(summary = currentSummary, comparison = comparison)
                        SalesSectionCard(
                            summary = currentSummary,
                            shopName = shopName,
                            onShare = {
                                shareCard(
                                    context = context,
                                    widthPx = widthPx,
                                    fileName = "sales_${shopName.replace(" ", "_")}_${selectedMonthId.replace(" ", "_")}.png",
                                    shareTitle = "Share Sales"
                                ) { SalesSectionCard(currentSummary, shopName) }
                            }
                        )
                        PurchaseSectionCard(
                            categories = purchaseChart,
                            statistics = purchaseStats,
                            shopName = shopName,
                            lastUpdated = currentSummary.lastUpdated,
                            onShare = {
                                shareCard(
                                    context = context,
                                    widthPx = widthPx,
                                    fileName = "purchase_${shopName.replace(" ", "_")}_${selectedMonthId.replace(" ", "_")}.png",
                                    shareTitle = "Share Purchase"
                                ) {
                                    PurchaseSectionCard(
                                        purchaseChart, purchaseStats, shopName,
                                        currentSummary.lastUpdated
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(navBarInset))
                    }
                }

                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No data for this month",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesSectionCard(
    summary: MonthlySummary,
    shopName: String = "",
    onShare: (() -> Unit)? = null
) {
    // Month-over-month delta now lives in MonthVerdictStrip above, so it appears once (D4).
    DetailSectionCard(
        title = "Sales",
        subtitle = shopName,
        caption = summary.lastUpdated.formatLastUpdated(),
        onShare = onShare
    ) { SummaryContent(summary = summary) }
}

@Composable
private fun PurchaseSectionCard(
    categories: List<PurchaseCategoryChartData>,
    statistics: PurchaseChartStatistics?,
    shopName: String = "",
    lastUpdated: Long = 0L,
    onShare: (() -> Unit)? = null
) {
    DetailSectionCard(
        title = "Purchase",
        subtitle = shopName,
        caption = lastUpdated.formatLastUpdated(),
        onShare = onShare,
        trailing = {
            // Budget variance, not month-over-month — an overrun is the bad direction here,
            // whichever way spend moved versus last month. See D7.
            if (statistics != null && statistics.totalTarget > 0.0) {
                DeltaChip(
                    deltaPercent = statistics.achievementPercentage - 100.0,
                    higherIsBetter = false
                )
            }
        }
    ) {
        PurchaseBreakdownSection(categories = categories, statistics = statistics)
    }
}

/**
 * Opens the screen with the month's verdict: gross margin as the headline (same figure, same
 * salesMarginColor thresholds as the Sales list card, so tapping a row keeps its identity),
 * the month-over-month delta, total sale and achievement on the right, and the full-width
 * margin bar underneath. Same surface/elevation as DetailSectionCard so it reads as part of
 * the same card family (D4).
 */
@Composable
private fun MonthVerdictStrip(summary: MonthlySummary, comparison: SalesComparison?) {
    val colors = MaterialTheme.colorScheme
    val status = semanticStatusColors()
    val totalSales = summary.totalSales
    val marginPct = if (totalSales > 0.0) {
        (totalSales - summary.totalPurchases) / totalSales * 100
    } else 0.0
    val marginColor = salesMarginColor(marginPct, status)
    val target = summary.targetSale
    val avg = summary.averageSale ?: 0.0
    val hasTarget = target > 0.0
    val achievementPct = if (hasTarget) avg / target * 100 else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(spaceLg)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GROSS MARGIN",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = "${"%.1f".format(marginPct)}%",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = marginColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    comparison?.totalSalesDeltaPct?.let { DeltaChip(deltaPercent = it) }
                    Spacer(modifier = Modifier.height(5.dp))
                    AedText(
                        amount = totalSales,
                        suffix = " sale",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                    if (hasTarget) {
                        Text(
                            text = "${achievementPct.roundToInt()}% of target",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(spaceMd))
            MarginBar(marginPct = marginPct)
        }
    }
}
