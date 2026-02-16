package com.marsa.smarttrackerhub.ui.screens.shops

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttrackerhub.utils.ShareUtil

@Composable
fun ShopsListScreen(
    onAddClick: () -> Unit,
    onEditClick: (Int) -> Unit
) {
    val viewModel: ShopListViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Store view references for each shop card
    val cardViewRefs = remember { mutableMapOf<Int, android.view.View>() }

    LaunchedEffect(Unit) {
        viewModel.initDatabase(context)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.shops.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No shops found")
                }
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
                    items(uiState.shops) { shop ->
                        // Wrap in AndroidView to get view reference
                        AndroidView(
                            factory = { context ->
                                androidx.compose.ui.platform.ComposeView(context).apply {
                                    setContent {
                                        ShopCard(
                                            shop = shop,
                                            onCardClick = { onEditClick(shop.id) },  // Changed parameter
                                            onShareClick = {
                                                cardViewRefs[shop.id]?.let { view ->
                                                    ShareUtil.shareViewAsImage(
                                                        view = view,
                                                        context = context,
                                                        fileName = "shop_info_${shop.shopName.replace(" ", "_")}.png",
                                                        shareTitle = "Share Shop Information"
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            },
                            update = { view ->
                                cardViewRefs[shop.id] = view
                                view.setContent {
                                    ShopCard(
                                        shop = shop,
                                        onCardClick = { onEditClick(shop.id) },
                                        onShareClick = {
                                            ShareUtil.shareViewAsImage(
                                                view = view,
                                                context = context,
                                                fileName = "shop_info_${shop.shopName.replace(" ", "_")}.png",
                                                shareTitle = "Share Shop Information"
                                            )
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
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
            Icon(Icons.Default.Add, contentDescription = "Add Shop")
        }
    }
}