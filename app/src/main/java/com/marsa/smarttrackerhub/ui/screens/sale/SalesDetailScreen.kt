package com.marsa.smarttrackerhub.ui.screens.sale

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.marsa.smarttrackerhub.ui.screens.purchase.PurchaseBreakdownSection
import com.marsa.smarttrackerhub.ui.screens.purchase.PurchaseItem

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
    val purchaseItems by viewModel.purchaseItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val comparison by viewModel.comparison.collectAsState()

    val idx = availableMonths.indexOfFirst { it.id == selectedMonthId }
    val canGoOlder = idx in 0 until (availableMonths.size - 1)
    val canGoNewer = idx > 0

    val density = LocalDensity.current
    val widthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = selectedMonthId,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (shopName.isNotBlank()) {
                            Text(
                                text = shopName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                    IconButton(
                        enabled = summary != null,
                        onClick = {
                            val activity = context as? ComponentActivity ?: return@IconButton
                            val currentSummary = summary ?: return@IconButton
                            val items = purchaseItems
                            com.marsa.smarttrackerhub.utils.ShareUtil.shareComposableAsImage(
                                activity = activity,
                                widthPx = widthPx,
                                fileName = "sales_${shopName.replace(" ", "_")}_${selectedMonthId.replace(" ", "_")}.png",
                                shareTitle = "Share Sales Detail"
                            ) {
                                SmartTrackerTheme {
                                    Surface {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            SalesSectionCard(currentSummary)
                                            PurchaseSectionCard(items)
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            MonthSwitcher(
                monthLabel = selectedMonthId,
                canGoOlder = canGoOlder,
                canGoNewer = canGoNewer,
                onOlder = { viewModel.goOlder() },
                onNewer = { viewModel.goNewer() }
            )

            when {
                isLoading && summary == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                summary != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SalesSectionCard(summary!!, comparison)
                        PurchaseSectionCard(purchaseItems)
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

@Composable
private fun MonthSwitcher(
    monthLabel: String,
    canGoOlder: Boolean,
    canGoNewer: Boolean,
    onOlder: () -> Unit,
    onNewer: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOlder, enabled = canGoOlder) {
            Icon(
                Icons.Default.KeyboardArrowLeft, contentDescription = "Older month",
                tint = if (canGoOlder) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
            )
        }
        Text(monthLabel, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
        IconButton(onClick = onNewer, enabled = canGoNewer) {
            Icon(
                Icons.Default.KeyboardArrowRight, contentDescription = "Newer month",
                tint = if (canGoNewer) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun SalesSectionCard(summary: MonthlySummary, comparison: SalesComparison? = null) {
    SectionShell(
        title = "Sales",
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
private fun PurchaseSectionCard(items: List<PurchaseItem>) {
    SectionShell("Purchase") { PurchaseBreakdownSection(purchases = items) }
}

@Composable
private fun SectionShell(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                trailing?.invoke()
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
