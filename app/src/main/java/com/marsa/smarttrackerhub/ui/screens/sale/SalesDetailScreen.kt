package com.marsa.smarttrackerhub.ui.screens.sale

import android.app.Application
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.marsa.smarttracker.ui.theme.SmartTrackerTheme
import com.marsa.smarttrackerhub.domain.MonthlySummary
import com.marsa.smarttrackerhub.ui.components.DeltaChip
import com.marsa.smarttrackerhub.ui.components.DropdownField
import com.marsa.smarttrackerhub.ui.screens.chart.PurchaseCategoryChartData
import com.marsa.smarttrackerhub.ui.screens.chart.PurchaseChartStatistics
import com.marsa.smarttrackerhub.ui.screens.purchase.PurchaseBreakdownSection
import com.marsa.smarttrackerhub.utils.ShareUtil

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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SalesSectionCard(
                            summary = currentSummary,
                            comparison = comparison,
                            onShare = {
                                shareCard(
                                    context = context,
                                    widthPx = widthPx,
                                    fileName = "sales_${shopName.replace(" ", "_")}_${selectedMonthId.replace(" ", "_")}.png",
                                    shareTitle = "Share Sales"
                                ) { SalesSectionCard(currentSummary, comparison) }
                            }
                        )
                        PurchaseSectionCard(
                            categories = purchaseChart,
                            statistics = purchaseStats,
                            onShare = {
                                shareCard(
                                    context = context,
                                    widthPx = widthPx,
                                    fileName = "purchase_${shopName.replace(" ", "_")}_${selectedMonthId.replace(" ", "_")}.png",
                                    shareTitle = "Share Purchase"
                                ) { PurchaseSectionCard(purchaseChart, purchaseStats) }
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
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

/** Renders [content] off-screen (themed) and shares it as an image. */
private fun shareCard(
    context: android.content.Context,
    widthPx: Int,
    fileName: String,
    shareTitle: String,
    content: @Composable () -> Unit
) {
    val activity = context as? ComponentActivity ?: return
    ShareUtil.shareComposableAsImage(
        activity = activity,
        widthPx = widthPx,
        fileName = fileName,
        shareTitle = shareTitle
    ) {
        SmartTrackerTheme {
            Surface {
                Box(modifier = Modifier.padding(16.dp)) { content() }
            }
        }
    }
}

@Composable
private fun SalesSectionCard(
    summary: MonthlySummary,
    comparison: SalesComparison? = null,
    onShare: (() -> Unit)? = null
) {
    SectionShell(
        title = "Sales",
        onShare = onShare,
        trailing = {
            val delta = comparison?.totalSalesDeltaPct
            if (delta != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DeltaChip(deltaPercent = delta)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "vs ${comparison.referenceLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { SummaryContent(summary = summary) }
}

@Composable
private fun PurchaseSectionCard(
    categories: List<PurchaseCategoryChartData>,
    statistics: PurchaseChartStatistics?,
    onShare: (() -> Unit)? = null
) {
    SectionShell(title = "Purchase", onShare = onShare) {
        PurchaseBreakdownSection(categories = categories, statistics = statistics)
    }
}

@Composable
private fun SectionShell(
    title: String,
    onShare: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    // Chrome matches the home cards: header row (16dp) → full-width divider → padded content.
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                trailing?.invoke()
                if (onShare != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share $title",
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = colors.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                content()
            }
        }
    }
}
