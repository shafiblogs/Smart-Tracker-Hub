package com.marsa.smarttrackerhub.ui.screens.shops

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

class ShopListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ShopListUiState(isLoading = true))
    val uiState: StateFlow<ShopListUiState> = _uiState.asStateFlow()

    private lateinit var repository: ShopRepository

    fun initDatabase(context: Context) {
        val db = AppDatabase.getDatabase(context)
        repository = ShopRepository(db.shopDao())
        loadShops()
    }

    private fun loadShops() = viewModelScope.launch {
        repository.getAllShops().collect { shopList ->
            _uiState.value = ShopListUiState(
                shops = shopList,
                isLoading = false
            )
        }
    }
}