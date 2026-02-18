package com.marsa.smarttrackerhub.ui.screens.investers

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.InvestorInfo
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import com.marsa.smarttrackerhub.data.entity.ShopInvestor
import com.marsa.smarttrackerhub.data.repository.InvestorRepository
import com.marsa.smarttrackerhub.data.repository.ShopInvestorRepository
import com.marsa.smarttrackerhub.data.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Assigns an investor to a shop with a fixed share %.
 * Actual money contributions are recorded separately via AddTransactionScreen.
 *
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

data class ShopInvestmentFormState(
    val selectedShopId: Int? = null,
    val selectedShopName: String = "",
    val selectedInvestorId: Int? = null,
    val selectedInvestorName: String = "",
    val sharePercentage: String = "",
    val shareError: String? = null,
    val shopError: String? = null,
    val investorError: String? = null
)

class AddShopInvestmentViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _formState = MutableStateFlow(ShopInvestmentFormState())
    val formState: StateFlow<ShopInvestmentFormState> = _formState.asStateFlow()

    private val _shops = MutableStateFlow<List<ShopInfo>>(emptyList())
    val shops: StateFlow<List<ShopInfo>> = _shops.asStateFlow()

    private val _investors = MutableStateFlow<List<InvestorInfo>>(emptyList())
    val investors: StateFlow<List<InvestorInfo>> = _investors.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** Remaining % available to assign in the selected shop. */
    private val _remainingPercentage = MutableStateFlow(100.0)
    val remainingPercentage: StateFlow<Double> = _remainingPercentage.asStateFlow()

    val isFormValid: StateFlow<Boolean> = _formState
        .map {
            it.selectedShopId != null &&
                    it.selectedInvestorId != null &&
                    it.sharePercentage.isNotBlank() &&
                    (it.sharePercentage.toDoubleOrNull() ?: 0.0) > 0.0
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private lateinit var shopInvestorRepo: ShopInvestorRepository
    private lateinit var shopRepo: ShopRepository
    private lateinit var investorRepo: InvestorRepository

    /**
     * @param prefilledInvestorId > 0 → coming from InvestorDetailScreen (investor locked)
     * @param prefilledShopId     > 0 → coming from ShopInvestmentDashboard (shop locked)
     */
    fun initDatabase(context: Context, prefilledInvestorId: Int = 0, prefilledShopId: Int = 0) {
        val db = AppDatabase.getDatabase(context)
        shopInvestorRepo = ShopInvestorRepository(db.shopInvestorDao())
        shopRepo = ShopRepository(db.shopDao())
        investorRepo = InvestorRepository(db.investorDao())

        loadShops()
        loadInvestors()

        if (prefilledInvestorId > 0) {
            viewModelScope.launch {
                investorRepo.getInvestorById(prefilledInvestorId)
                    ?.let { selectInvestor(it.id, it.investorName) }
            }
        }
        if (prefilledShopId > 0) {
            viewModelScope.launch {
                shopRepo.getShopById(prefilledShopId)
                    ?.let { selectShop(it.id, it.shopName) }
            }
        }
    }

    private fun loadShops() = viewModelScope.launch {
        shopRepo.getAllShops().collect { _shops.value = it }
    }

    private fun loadInvestors() = viewModelScope.launch {
        investorRepo.getAllInvestors().collect { _investors.value = it }
    }

    fun selectShop(shopId: Int, shopName: String) {
        _formState.update { it.copy(selectedShopId = shopId, selectedShopName = shopName, shopError = null) }
        loadRemaining(shopId)
    }

    fun selectInvestor(investorId: Int, investorName: String) {
        _formState.update { it.copy(selectedInvestorId = investorId, selectedInvestorName = investorName, investorError = null) }
    }

    private fun loadRemaining(shopId: Int) = viewModelScope.launch {
        val allocated = shopInvestorRepo.getTotalPercentageForShop(shopId)
        _remainingPercentage.value = 100.0 - allocated
    }

    fun updateSharePercentage(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(sharePercentage = value, shareError = null) }
        }
    }

    fun saveAssignment(
        context: Context,
        onSuccess: () -> Unit = {},
        onFail: (String) -> Unit = {}
    ) = viewModelScope.launch {
        val state = _formState.value

        val shopId = state.selectedShopId ?: run {
            _formState.update { it.copy(shopError = "Please select a shop") }
            return@launch
        }
        val investorId = state.selectedInvestorId ?: run {
            _formState.update { it.copy(investorError = "Please select an investor") }
            return@launch
        }
        val share = state.sharePercentage.toDoubleOrNull()
        if (share == null || share <= 0.0) {
            _formState.update { it.copy(shareError = "Enter a valid share percentage") }
            return@launch
        }
        if (share > _remainingPercentage.value) {
            _formState.update {
                it.copy(shareError = "Only ${String.format("%.1f", _remainingPercentage.value)}% remaining")
            }
            return@launch
        }
        if (shopInvestorRepo.isInvestorInShop(shopId, investorId)) {
            onFail("This investor is already assigned to the selected shop")
            return@launch
        }

        try {
            shopInvestorRepo.insertShopInvestor(
                ShopInvestor(
                    shopId = shopId,
                    investorId = investorId,
                    sharePercentage = share
                )
            )
            _isSaved.value = true
            onSuccess()
        } catch (e: Exception) {
            onFail("Failed to save: ${e.localizedMessage}")
        }
    }
}
