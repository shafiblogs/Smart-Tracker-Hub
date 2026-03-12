package com.marsa.smarttrackerhub.ui.screens.shops

import com.marsa.smarttrackerhub.data.entity.ShopInfo


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

data class ShopListUiState(
    val shops: List<ShopInfo> = emptyList(),
    val isLoading: Boolean = false
)