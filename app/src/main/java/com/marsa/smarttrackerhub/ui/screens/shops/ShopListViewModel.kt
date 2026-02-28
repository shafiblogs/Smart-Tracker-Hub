package com.marsa.smarttrackerhub.ui.screens.shops

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.repository.ShopInvestorRepository
import com.marsa.smarttrackerhub.data.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    private lateinit var investorRepository: ShopInvestorRepository

    fun initDatabase(context: Context) {
        val db = AppDatabase.getDatabase(context)
        repository = ShopRepository(db.shopDao())
        investorRepository = ShopInvestorRepository(db.shopInvestorDao())
        loadShops()
    }

    private fun loadShops() = viewModelScope.launch {
        repository.getAllShops()
            .flatMapLatest { shopList ->
                if (shopList.isEmpty()) {
                    flowOf(Pair(shopList, emptyMap<Int, Double>()))
                } else {
                    val investedFlows = shopList.map { shop ->
                        investorRepository.getTotalInvestedForShop(shop.id)
                            .flatMapLatest { total -> flowOf(shop.id to total) }
                    }
                    combine(investedFlows) { pairs ->
                        Pair(shopList, pairs.toMap())
                    }
                }
            }
            .collect { (shopList, investedMap) ->
                _uiState.value = ShopListUiState(
                    shops = shopList,
                    totalInvestedByShop = investedMap,
                    isLoading = false
                )
            }
    }
}