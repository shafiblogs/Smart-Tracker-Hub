package com.marsa.smarttrackerhub.ui.screens.investers

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.repository.InvestorRepository
import com.marsa.smarttrackerhub.data.repository.ShopInvestorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Created by Muhammed Shafi on 17/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class InvestorDetailViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvestorDetailUiState())
    val uiState: StateFlow<InvestorDetailUiState> = _uiState.asStateFlow()

    fun init(context: Context, investorId: Int) {
        val db = AppDatabase.getDatabase(context)
        val investorRepo = InvestorRepository(db.investorDao())
        val shopInvestorRepo = ShopInvestorRepository(db.shopInvestorDao())

        // Load investor info + totals once
        viewModelScope.launch {
            try {
                val investor = investorRepo.getInvestorById(investorId)
                val totalInvested = shopInvestorRepo.getTotalInvestedByInvestor(investorId)
                val shopCount = shopInvestorRepo.getShopCountForInvestor(investorId)
                _uiState.update {
                    it.copy(
                        investor = investor,
                        totalInvested = totalInvested,
                        shopCount = shopCount
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }

        // Collect shop investments as a Flow so list updates live
        viewModelScope.launch {
            try {
                shopInvestorRepo.getShopsForInvestor(investorId).collect { shops ->
                    _uiState.update { it.copy(shopInvestments = shops, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }
}
