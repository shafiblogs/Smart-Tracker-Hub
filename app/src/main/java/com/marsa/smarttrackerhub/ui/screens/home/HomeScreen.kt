package com.marsa.smarttrackerhub.ui.screens.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.marsa.smarttracker.ui.theme.semanticStatusColors
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.domain.AccountSummary
import com.marsa.smarttrackerhub.ui.components.AedText
import com.marsa.smarttrackerhub.ui.components.DetailSectionCard
import com.marsa.smarttrackerhub.ui.components.MonthSelector
import com.marsa.smarttrackerhub.ui.screens.chart.UnifiedStatisticsCard
import com.marsa.smarttrackerhub.ui.screens.chart.MoneyAllocationBar
import com.marsa.smarttrackerhub.utils.formatLastUpdated
import com.marsa.smarttrackerhub.utils.shareCard

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

    val availableRanges by viewModel.availableRanges.collectAsState()
    val selectedRange by viewModel.selectedRange.collectAsState()
    val accountCards by viewModel.accountCards.collectAsState()
    val shopStats by viewModel.shopStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val widthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }

    LaunchedEffect(Unit) { viewModel.loadScreenData(userAccessCode) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ── Loading progress bar on top ────────────────────────────────────────
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Period selector (drives everything below) ────────────────────────
            MonthSelector(
                label = "Period",
                selection = selectedRange,
                presets = availableRanges,
                onSelectionChange = { viewModel.setSelectedRange(it) },
                enabled = availableRanges.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (accountCards.isEmpty() && shopStats.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
            // ── Account card(s) — region-level aggregate, on top ─────────────
            accountCards.forEach { ra ->
                val summary = ra.summary
                val regionName = ra.region.name ?: ""
                DetailSectionCard(
                    title = regionName,
                    subtitle = selectedRange.displayName,
                    caption = summary?.lastUpdated?.formatLastUpdated(),
                    onShare = summary?.let {
                        {
                            shareCard(
                                context, widthPx,
                                "account_${regionName.replace(" ", "_")}_${selectedRange.displayName.replace(" ", "_")}.png",
                                "Share Account"
                            ) { HomeAccountTiles(it) }
                        }
                    }
                ) {
                    if (summary != null) {
                        HomeAccountTiles(summary)
                    } else {
                        Text(
                            text = "No account data for ${selectedRange.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // ── Sales card per shop (same UnifiedStatisticsCard as the old Home) ──
            shopStats.forEach { st ->
                val sales = st.sales
                val purchase = st.purchase
                val shopName = st.shop.name ?: ""
                if (sales != null && purchase != null) {
                    UnifiedStatisticsCard(
                        salesStatistics = sales,
                        purchaseStatistics = purchase,
                        shopName = shopName,
                        periodLabel = selectedRange.displayName,
                        salesMargin = st.salesMargin,
                        caption = st.lastUpdated.takeIf { it > 0 }?.formatLastUpdated(),
                        onShareClick = {
                            shareCard(
                                context, widthPx,
                                "sales_${shopName.replace(" ", "_")}_${selectedRange.displayName.replace(" ", "_")}.png",
                                "Share Sales"
                            ) {
                                UnifiedStatisticsCard(
                                    salesStatistics = sales,
                                    purchaseStatistics = purchase,
                                    shopName = shopName,
                                    periodLabel = selectedRange.displayName,
                                    salesMargin = st.salesMargin,
                                    caption = st.lastUpdated.takeIf { it > 0 }?.formatLastUpdated()
                                )
                            }
                        }
                    )
                } else {
                    DetailSectionCard(title = "Sales", subtitle = shopName) {
                        Text(
                            text = "No sales data for ${selectedRange.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Home account card redesign:
 *  - Collection: full-width headline (largest type, no ellipsis)
 *  - Gross Profit + Net Profit: two-up row with inline margins
 *  - Money allocation bar + legend: simplified to one hue stepped by opacity
 */
@Composable
private fun HomeAccountTiles(summary: AccountSummary) {
    val colors = MaterialTheme.colorScheme
    val status = semanticStatusColors()
    Column(modifier = Modifier.fillMaxWidth()) {
        // Collection headline
        Text(
            "COLLECTION",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.06f.sp),
            color = colors.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        AedText(
            amount = summary.totalCollection,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp),
            color = colors.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Gross Profit + Net Profit two-up row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Gross Profit
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Gross Profit",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AedText(
                        amount = summary.grossProfit,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (summary.grossProfit >= 0) status.success else status.danger
                    )
                    Text(
                        "${"%.0f".format(summary.grossMargin)}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (summary.grossProfit >= 0) status.success else status.danger
                    )
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(colors.outlineVariant)
            )

            // Net Profit
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Net Profit",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AedText(
                        amount = summary.netProfit,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (summary.netProfit >= 0) status.success else status.danger
                    )
                    Text(
                        "${"%.0f".format(summary.netProfitMargin)}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (summary.netProfit >= 0) status.success else status.danger
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = colors.outlineVariant)
        Spacer(modifier = Modifier.height(12.dp))

        // Money allocation bar + legend
        MoneyAllocationBar(
            collection = summary.totalCollection,
            purchase = summary.totalPurchases,
            expense = summary.totalExpenses,
            withdrawal = summary.withdrawal,
            provision = summary.provision,
            outstandingPayments = summary.outstandingPayments
        )
    }
}
