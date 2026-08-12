package com.marsa.smarttrackerhub.ui.screens.summary

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.marsa.smarttrackerhub.domain.AccountSummary
import com.marsa.smarttrackerhub.ui.components.DeltaChip
import com.marsa.smarttrackerhub.ui.components.DetailSectionCard
import com.marsa.smarttrackerhub.ui.components.DropdownField
import com.marsa.smarttrackerhub.utils.formatLastUpdated
import com.marsa.smarttrackerhub.utils.shareCard
import com.marsa.smarttracker.ui.theme.navBarInset
import com.marsa.smarttracker.ui.theme.spaceLg
import com.marsa.smarttracker.ui.theme.spaceMd
import com.marsa.smarttracker.ui.theme.spaceSm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    shopId: String,
    monthId: String,
    shopName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val firebaseApp = FirebaseApp.getInstance("AccountTrackerApp")
    val viewModel: AccountDetailViewModel = viewModel(
        factory = AccountDetailViewModelFactory(
            context.applicationContext as Application, firebaseApp, shopId, monthId
        )
    )

    val availableMonths by viewModel.availableMonths.collectAsState()
    val selectedMonthId by viewModel.selectedMonthId.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val comparison by viewModel.comparison.collectAsState()
    val history by viewModel.history.collectAsState()

    val density = LocalDensity.current
    val widthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }

    // Hoisted (not scoped to the current month) so switching months doesn't reset the lens —
    // survives rotation via rememberSaveable too (D9).
    var moneyMovedView by rememberSaveable { mutableStateOf(MoneyMovedView.FLOW) }

    Scaffold(
        // Outer NavHost Scaffold already applies the status-bar inset; don't double it here.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = shopName.ifBlank { "Account" },
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
                        SummaryCard(
                            summary = currentSummary,
                            comparison = comparison,
                            shopName = shopName,
                            onShare = {
                                shareCard(
                                    context, widthPx,
                                    "account_summary_${shopName.replace(" ", "_")}_${selectedMonthId.replace(" ", "_")}.png",
                                    "Share Account Summary"
                                ) { SummaryCard(currentSummary, comparison, shopName) }
                            }
                        )
                        MoneyMovedCard(
                            summary = currentSummary,
                            shopName = shopName,
                            selected = moneyMovedView,
                            onSelectedChange = { moneyMovedView = it },
                            onShare = {
                                val view = moneyMovedView
                                shareCard(
                                    context, widthPx,
                                    "account_${view.label.lowercase()}_${shopName.replace(" ", "_")}_${selectedMonthId.replace(" ", "_")}.png",
                                    "Share ${view.label}"
                                ) { MoneyMovedCard(currentSummary, shopName, view, {}) }
                            }
                        )
                        ProfitTrendCard(
                            history = history,
                            shopName = shopName,
                            onShare = {
                                shareCard(
                                    context, widthPx,
                                    "account_trend_${shopName.replace(" ", "_")}.png",
                                    "Share Profit Trend"
                                ) { ProfitTrendCard(history, shopName) }
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
private fun SummaryCard(
    summary: AccountSummary,
    comparison: AccountComparison? = null,
    shopName: String = "",
    onShare: (() -> Unit)? = null
) {
    // Cash-balance delta moved down into AccountProfitTiles as the Balance tile's own
    // opening-to-closing caption; only the net-profit chip stays up here, on the title line
    // where SalesDetailScreen already positions its equivalent (D8).
    DetailSectionCard(
        title = "Summary",
        subtitle = shopName,
        caption = summary.lastUpdated.formatLastUpdated(),
        onShare = onShare,
        trailing = {
            comparison?.netProfitDeltaPct?.let { DeltaChip(deltaPercent = it) }
        }
    ) {
        AccountProfitTiles(summary)
    }
}

