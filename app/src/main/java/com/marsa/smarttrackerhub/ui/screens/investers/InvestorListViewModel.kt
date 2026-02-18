package com.marsa.smarttrackerhub.ui.screens.investers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.repository.InvestorRepository
import com.marsa.smarttrackerhub.data.repository.ShopInvestorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class InvestorListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(InvestorListUiState(isLoading = true))
    val uiState: StateFlow<InvestorListUiState> = _uiState

    private lateinit var investorRepo: InvestorRepository
    private lateinit var shopInvestorRepo: ShopInvestorRepository

    fun initDatabase(context: Context) {
        val db = AppDatabase.getDatabase(context)
        investorRepo = InvestorRepository(db.investorDao())
        shopInvestorRepo = ShopInvestorRepository(db.shopInvestorDao())
        loadInvestors()
    }

    private fun loadInvestors() = viewModelScope.launch {
        investorRepo.getAllInvestors().collect { list ->
            // Load portfolio totals for each investor
            val totals = list.associate { investor ->
                val totalInvested = shopInvestorRepo.getTotalInvestedByInvestor(investor.id)
                val shopCount = shopInvestorRepo.getShopCountForInvestor(investor.id)
                investor.id to Pair(totalInvested, shopCount)
            }
            _uiState.value = InvestorListUiState(
                investors = list,
                portfolioTotals = totals,
                isLoading = false
            )
        }
    }
}