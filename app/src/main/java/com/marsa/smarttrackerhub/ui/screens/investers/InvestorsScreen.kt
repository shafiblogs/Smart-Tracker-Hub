package com.marsa.smarttrackerhub.ui.screens.investers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel


/**
 * Created by Muhammed Shafi on 11/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

@Composable
fun InvestorsScreen(onAddClick: () -> Unit, onItemClick: (Int) -> Unit) {
    val viewModel: InvestorListViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.initDatabase(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            uiState.investors.isEmpty() -> {
                Text(
                    "No investors found",
                    Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.investors) { investor ->
                        val (totalInvested, shopCount) =
                            uiState.portfolioTotals[investor.id] ?: Pair(0.0, 0)
                        InvestorCard(
                            investor = investor,
                            totalInvested = totalInvested,
                            shopCount = shopCount,
                            onCardClick = { onItemClick(investor.id) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Investor")
        }
    }
}