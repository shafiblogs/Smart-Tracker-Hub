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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.marsa.smarttracker.ui.theme.semanticStatusColors
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.domain.AccountSummary
import com.marsa.smarttrackerhub.ui.components.DetailSectionCard
import com.marsa.smarttrackerhub.ui.components.DropdownField
import com.marsa.smarttrackerhub.ui.screens.chart.UnifiedStatisticsCard
import com.marsa.smarttrackerhub.utils.formatMoney

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
                DetailSectionCard(title = "Account", subtitle = ra.region.name ?: "") {
                    val summary = ra.summary
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
                if (sales != null && purchase != null) {
                    UnifiedStatisticsCard(
                        salesStatistics = sales,
                        purchaseStatistics = purchase,
                        shopName = st.shop.name ?: "",
                        periodLabel = selectedRange.displayName,
                        salesMargin = st.salesMargin
                    )
                } else {
                    DetailSectionCard(title = "Sales", subtitle = st.shop.name ?: "") {
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
 * Home account card:
 *  - two headline profit tiles (Gross / Net, sign-coloured, with margin), then
 *  - a divider + an aligned amount list (Collection / Purchase / Expense / Withdrawal / Provision).
 */
@Composable
private fun HomeAccountTiles(summary: AccountSummary) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SignedTile("Gross Profit", summary.grossProfit, summary.grossMargin, Modifier.weight(1f))
            SignedTile("Net Profit", summary.netProfit, summary.netProfitMargin, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = colors.outlineVariant)
        Spacer(modifier = Modifier.height(6.dp))

        AccountLineRow("Collection", summary.totalCollection, colors.primary)
        AccountLineRow("Purchase", summary.totalPurchases, colors.error)
        AccountLineRow("Expense", summary.totalExpenses, colors.error)
        AccountLineRow("Withdrawal", summary.withdrawal, colors.error)
        AccountLineRow("Provision", summary.provision, colors.secondary)
    }
}

@Composable
private fun SignedTile(label: String, amount: Double, marginPercent: Double, modifier: Modifier = Modifier) {
    val status = semanticStatusColors()
    val color = if (amount >= 0) status.success else status.danger
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            formatMoney(amount, 0),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            "${"%.0f".format(marginPercent)}% margin",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Label (left) + amount (right) — always visible, even when the value is 0. */
@Composable
private fun AccountLineRow(label: String, amount: Double, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            formatMoney(amount, 0),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor
        )
    }
}
