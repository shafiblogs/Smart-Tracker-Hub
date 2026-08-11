package com.marsa.smarttrackerhub.ui.screens.investments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttrackerhub.ui.components.DetailSectionCard
import com.marsa.smarttrackerhub.utils.formatMoney

/**
 * Shop-first Investments dashboard (bottom tab). A portfolio summary strip over a list of
 * shops, each showing capital / investors / allocated %. Tapping a shop opens the existing
 * ShopInvestmentDashboard (full breakdown + entry). Display-only; entry stays unchanged.
 */
@Composable
fun InvestmentsScreen(
    onShopClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val viewModel: InvestmentsViewModel = viewModel()
    LaunchedEffect(Unit) { viewModel.init(context) }
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { PortfolioSummaryCard(uiState) }

                if (uiState.shops.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No investment shops yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(uiState.shops) { row ->
                        InvestmentShopCard(
                            row = row,
                            totalPortfolioCapital = uiState.totalCapital,
                            onClick = { onShopClick(row.shopId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortfolioSummaryCard(state: InvestmentsUiState) {
    DetailSectionCard(title = "Portfolio") {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatMoney(state.totalCapital, 0),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${state.shopCount} shops · ${state.investorCount} investors",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            CapitalDistributionBar(
                shops = state.shops,
                totalCapital = state.totalCapital
            )
        }
    }
}
