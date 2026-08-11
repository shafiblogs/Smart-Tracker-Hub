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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.marsa.smarttracker.ui.theme.semanticStatusColors
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.domain.AccountSummary
import com.marsa.smarttrackerhub.ui.components.DetailSectionCard
import com.marsa.smarttrackerhub.ui.components.DropdownField
import com.marsa.smarttrackerhub.ui.screens.chart.UnifiedStatisticsCard
import com.marsa.smarttrackerhub.ui.screens.chart.MoneyAllocationBar
import com.marsa.smarttrackerhub.utils.formatLastUpdated
import com.marsa.smarttrackerhub.utils.formatMoney
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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ── Period selector (drives everything below) ────────────────────────
        DropdownField(
            label = "Period",
            selectedValue = selectedRange.displayName,
            options = availableRanges.map { it.displayName },
            onOptionSelected = { name ->
                availableRanges.firstOrNull { it.displayName == name }
                    ?.let { viewModel.setSelectedRange(it) }
            },
            enabled = availableRanges.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading && accountCards.isEmpty() && shopStats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
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

/**
 * Home account card redesign:
 *  - Three headline tiles: Collection (neutral), Gross Profit (sign-coloured, with margin), Net
 *    Profit (sign-coloured, with margin)
 *  - A segmented bar + legend showing Collection split across Purchase / Expense / Withdrawal /
 *    Provision / Out Payment / Retained — this is the single source of truth for the breakdown,
 *    so the figures aren't repeated in a separate list below it.
 */
@Composable
private fun HomeAccountTiles(summary: AccountSummary) {
    val colors = MaterialTheme.colorScheme
    val status = semanticStatusColors()
    Column(modifier = Modifier.fillMaxWidth()) {
        // Three headline tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccountTile("Collection", formatMoney(summary.totalCollection, 0), colors.primary, modifier = Modifier.weight(1f))
            AccountTile(
                "Gross Profit",
                formatMoney(summary.grossProfit, 0),
                if (summary.grossProfit >= 0) status.success else status.danger,
                marginPercent = summary.grossMargin,
                modifier = Modifier.weight(1f)
            )
            AccountTile(
                "Net Profit",
                formatMoney(summary.netProfit, 0),
                if (summary.netProfit >= 0) status.success else status.danger,
                marginPercent = summary.netProfitMargin,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = colors.outlineVariant)
        Spacer(modifier = Modifier.height(12.dp))

        // Money allocation bar + legend: Collection split across categories, Out Payment folded
        // into the same reconciliation as Retained.
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

/** Headline tile: label + formatted amount, with an optional margin % (Gross/Net only). */
@Composable
private fun AccountTile(
    label: String,
    value: String,
    valueColor: Color,
    marginPercent: Double? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        if (marginPercent != null) {
            Text(
                "${"%.0f".format(marginPercent)}% margin",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
