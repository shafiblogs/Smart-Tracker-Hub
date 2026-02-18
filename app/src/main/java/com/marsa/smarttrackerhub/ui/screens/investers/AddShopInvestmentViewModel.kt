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
 * Created by Muhammed Shafi on 18/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

data class ShopInvestmentFormState(
    val selectedShopId: Int? = null,
    val selectedShopName: String = "",
    val selectedInvestorId: Int? = null,
    val selectedInvestorName: String = "",
    val sharePercentage: String = "",
    val investmentAmount: String = "",
    val investmentDate: Long = System.currentTimeMillis(),
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

    // Used to show remaining % available in the selected shop
    private val _allocatedPercentage = MutableStateFlow(0.0)
    val allocatedPercentage: StateFlow<Double> = _allocatedPercentage.asStateFlow()

    val isFormValid: StateFlow<Boolean> = formState
        .map {
            it.selectedShopId != null &&
                    it.selectedInvestorId != null &&
                    it.sharePercentage.isNotBlank() &&
                    (it.sharePercentage.toDoubleOrNull() ?: 0.0) > 0.0 &&
                    it.investmentAmount.isNotBlank() &&
                    (it.investmentAmount.toDoubleOrNull() ?: 0.0) > 0.0
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private lateinit var shopInvestorRepo: ShopInvestorRepository
    private lateinit var shopRepo: ShopRepository
    private lateinit var investorRepo: InvestorRepository

    /**
     * @param prefilledInvestorId  > 0 when arriving from InvestorDetailScreen (investor is fixed)
     * @param prefilledShopId      > 0 when arriving from AddShopScreen (shop is fixed)
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
                val investor = investorRepo.getInvestorById(prefilledInvestorId)
                investor?.let { selectInvestor(it.id, it.investorName) }
            }
        }

        if (prefilledShopId > 0) {
            viewModelScope.launch {
                val shop = shopRepo.getShopById(prefilledShopId)
                shop?.let { selectShop(it.id, it.shopName) }
            }
        }
    }

    private fun loadShops() = viewModelScope.launch {
        shopRepo.getAllShops().collect { list ->
            _shops.value = list
        }
    }

    private fun loadInvestors() = viewModelScope.launch {
        investorRepo.getAllInvestors().collect { list ->
            _investors.value = list
        }
    }

    fun selectShop(shopId: Int, shopName: String) {
        _formState.update { it.copy(selectedShopId = shopId, selectedShopName = shopName, shopError = null) }
        loadAllocatedPercentage(shopId)
    }

    fun selectInvestor(investorId: Int, investorName: String) {
        _formState.update { it.copy(selectedInvestorId = investorId, selectedInvestorName = investorName, investorError = null) }
    }

    private fun loadAllocatedPercentage(shopId: Int) = viewModelScope.launch {
        _allocatedPercentage.value = shopInvestorRepo.getTotalPercentageForShop(shopId)
    }

    fun updateSharePercentage(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(sharePercentage = value, shareError = null) }
        }
    }

    fun updateInvestmentAmount(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(investmentAmount = value) }
        }
    }

    fun updateInvestmentDate(dateInMillis: Long) {
        _formState.update { it.copy(investmentDate = dateInMillis) }
    }

    fun saveInvestment(
        context: Context,
        onSuccess: () -> Unit = {},
        onFail: (String) -> Unit = {}
    ) = viewModelScope.launch {
        val state = _formState.value

        val shopId = state.selectedShopId
        if (shopId == null) {
            _formState.update { it.copy(shopError = "Please select a shop") }
            return@launch
        }

        val investorId = state.selectedInvestorId
        if (investorId == null) {
            _formState.update { it.copy(investorError = "Please select an investor") }
            return@launch
        }

        val share = state.sharePercentage.toDoubleOrNull()
        if (share == null || share <= 0.0) {
            _formState.update { it.copy(shareError = "Enter a valid share percentage") }
            return@launch
        }

        val remaining = 100.0 - _allocatedPercentage.value
        if (share > remaining) {
            _formState.update { it.copy(shareError = "Only ${String.format("%.1f", remaining)}% remaining for this shop") }
            return@launch
        }

        val amount = state.investmentAmount.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _error.value = "Enter a valid investment amount"
            return@launch
        }

        // Check if this investor already has an entry for this shop
        val alreadyExists = shopInvestorRepo.isInvestorInShop(shopId, investorId)
        if (alreadyExists) {
            onFail("This investor already has an investment in the selected shop")
            return@launch
        }

        try {
            val shopInvestor = ShopInvestor(
                shopId = shopId,
                investorId = investorId,
                sharePercentage = share,
                investmentAmount = amount,
                investmentDate = state.investmentDate
            )
            shopInvestorRepo.insertShopInvestor(shopInvestor)
            _isSaved.value = true
            onSuccess()
        } catch (e: Exception) {
            onFail("Failed to save investment: ${e.localizedMessage}")
        }
    }
}
