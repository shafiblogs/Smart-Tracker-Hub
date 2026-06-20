package com.marsa.smarttrackerhub.ui.screens.purchase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.utils.ShareUtil

/**
 * Displays category-wise purchase breakdown per month, per shop —
 * data sourced from the `purchaseBreakdown` field in SmartTracker's
 * Firestore summary/{shopId}/months/{monthYear} document.
 *
 * Created by Muhammed Shafi on 13/03/2026.
 * Moro Hub
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(userAccessCode: AccessCode) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val firebaseApp = FirebaseApp.getInstance("SmartTrackerApp")
    val viewModel: PurchaseScreenViewModel = viewModel(
        factory = PurchaseScreenViewModelFactory(application, firebaseApp)
    )

    LaunchedEffect(userAccessCode) {
        viewModel.loadScreenData(userAccessCode)
    }

    val shops by viewModel.shops.collectAsState()
    val selectedShop by viewModel.selectedShop.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val selectedMonthId by viewModel.selectedMonthId.collectAsState()
    val purchaseCache by viewModel.purchaseCache.collectAsState()
    val lastUpdatedCache by viewModel.lastUpdatedCache.collectAsState()
    val expanded by viewModel.expanded.collectAsState()
    val isLoadingMonth by viewModel.isLoadingMonth.collectAsState()

    val cardViewRefs = remember { mutableMapOf<String, android.view.View>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ── Shop dropdown ────────────────────────────────────────────────
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

        // ── Content area ─────────────────────────────────────────────────
        when {
            selectedShop == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select a shop to view purchases",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            availableMonths.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available for ${selectedShop?.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableMonths) { monthItem ->
                        val isSelected = selectedMonthId == monthItem.id
                        val purchases = purchaseCache[monthItem.id]
                        val lastUpdated = lastUpdatedCache[monthItem.id] ?: 0L

                        AndroidView(
                            factory = { ctx ->
                                androidx.compose.ui.platform.ComposeView(ctx).apply {
                                    setContent {
                                        PurchaseCard(
                                            monthItem = monthItem,
                                            isSelected = isSelected,
                                            purchases = purchases,
                                            isLoading = isSelected && isLoadingMonth,
                                            shopAddress = selectedShop?.name ?: "",
                                            lastUpdated = lastUpdated,
                                            onClick = { viewModel.selectMonth(monthItem.id) },
                                            onRefresh = { viewModel.refreshMonth(monthItem.id) },
                                            onShare = if (purchases != null && isSelected) {
                                                {
                                                    cardViewRefs[monthItem.id]?.let { view ->
                                                        ShareUtil.shareViewAsImage(
                                                            view = view,
                                                            context = ctx,
                                                            fileName = "purchases_${selectedShop?.name?.replace(" ", "_")}_${monthItem.displayName.replace(" ", "_")}.png",
                                                            shareTitle = "Share Purchase Summary"
                                                        )
                                                    }
                                                }
                                            } else null
                                        )
                                    }
                                }
                            },
                            update = { view ->
                                if (isSelected) cardViewRefs[monthItem.id] = view
                                view.setContent {
                                    PurchaseCard(
                                        monthItem = monthItem,
                                        isSelected = isSelected,
                                        purchases = purchases,
                                        isLoading = isSelected && isLoadingMonth,
                                        shopAddress = selectedShop?.name ?: "",
                                        lastUpdated = lastUpdated,
                                        onClick = { viewModel.selectMonth(monthItem.id) },
                                        onRefresh = { viewModel.refreshMonth(monthItem.id) },
                                        onShare = if (purchases != null && isSelected) {
                                            {
                                                ShareUtil.shareViewAsImage(
                                                    view = view,
                                                    context = context,
                                                    fileName = "purchases_${selectedShop?.name?.replace(" ", "_")}_${monthItem.displayName.replace(" ", "_")}.png",
                                                    shareTitle = "Share Purchase Summary"
                                                )
                                            }
                                        } else null
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
