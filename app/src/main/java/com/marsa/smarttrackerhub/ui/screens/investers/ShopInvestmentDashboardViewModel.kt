package com.marsa.smarttrackerhub.ui.screens.investers

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.repository.InvestmentTransactionRepository
import com.marsa.smarttrackerhub.data.repository.ShopInvestorRepository
import com.marsa.smarttrackerhub.data.repository.ShopRepository
import com.marsa.smarttrackerhub.domain.PhaseTransactionDetail
import com.marsa.smarttrackerhub.domain.ShopInvestorSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

data class ShopInvestmentDashboardUiState(
    val shopName: String = "",
    val totalCapital: Double = 0.0,
    val allocatedPercentage: Double = 0.0,
    val investorCount: Int = 0,
    val investors: List<ShopInvestorSummary> = emptyList(),
    val transactions: List<PhaseTransactionDetail> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class ShopInvestmentDashboardViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopInvestmentDashboardUiState())
    val uiState: StateFlow<ShopInvestmentDashboardUiState> = _uiState.asStateFlow()

    fun init(context: Context, shopId: Int) {
        val db = AppDatabase.getDatabase(context)
        val shopRepo = ShopRepository(db.shopDao())
        val shopInvestorRepo = ShopInvestorRepository(db.shopInvestorDao())
        val txRepo = InvestmentTransactionRepository(db.investmentTransactionDao())

        viewModelScope.launch {
            try {
                val shop = shopRepo.getShopById(shopId)
                _uiState.value = _uiState.value.copy(shopName = shop?.shopName ?: "")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.localizedMessage)
            }
        }

        // Combine live investor list + live transaction list
        viewModelScope.launch {
            try {
                combine(
                    shopInvestorRepo.getInvestorsForShop(shopId),
                    txRepo.getTransactionsForShop(shopId)
                ) { investors, transactions ->
                    val totalCapital = transactions.sumOf { it.amount }
                    val allocatedPct = investors.sumOf { it.sharePercentage }
                    ShopInvestmentDashboardUiState(
                        shopName = _uiState.value.shopName,
                        totalCapital = totalCapital,
                        allocatedPercentage = allocatedPct,
                        investorCount = investors.size,
                        investors = investors,
                        transactions = transactions,
                        isLoading = false
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.localizedMessage, isLoading = false)
            }
        }
    }
}
