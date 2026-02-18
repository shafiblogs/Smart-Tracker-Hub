package com.marsa.smarttrackerhub.ui.screens.shops

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import com.marsa.smarttrackerhub.data.repository.ShopInvestorRepository
import com.marsa.smarttrackerhub.data.repository.ShopRepository
import com.marsa.smarttrackerhub.domain.ShopInvestorDetail
import com.marsa.smarttrackerhub.ui.screens.enums.ShopType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddShopViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _formState = MutableStateFlow(ShopFormState())
    val formState = _formState.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _shopInvestors = MutableStateFlow<List<ShopInvestorDetail>>(emptyList())
    val shopInvestors: StateFlow<List<ShopInvestorDetail>> = _shopInvestors.asStateFlow()

    private var editingShopId: Int? = null
    private var shopInvestorRepo: ShopInvestorRepository? = null

    val isFormValid: StateFlow<Boolean> = formState
        .map {
            it.shopName.isNotBlank() &&
                    it.shopId.isNotBlank() &&
                    it.shopAddress.isNotBlank() &&
                    it.licenseExpiryDate != null &&
                    it.shopOpeningDate != null &&
                    it.stockValue.isNotBlank() &&
                    it.stockTakenDate != null
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    // Calculated Zakath amount (5% of stock)
    val zakathAmount: StateFlow<Double> = formState
        .map {
            val stock = it.stockValue.toDoubleOrNull() ?: 0.0
            stock * 0.025
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    fun loadShop(context: Context, shopId: Int) = viewModelScope.launch {
        try {
            val db = AppDatabase.getDatabase(context)
            val repo = ShopRepository(db.shopDao())
            val investorRepo = ShopInvestorRepository(db.shopInvestorDao())
            shopInvestorRepo = investorRepo
            val shop = repo.getShopById(shopId)

            shop?.let {
                editingShopId = it.id
                _formState.value = ShopFormState(
                    shopName = it.shopName,
                    shopAddress = it.shopAddress,
                    shopId = it.shopId,
                    zakathStatus = ZakathStatus.valueOf(it.zakathStatus),
                    shopType = ShopType.valueOf(it.shopType),
                    licenseExpiryDate = it.licenseExpiryDate,
                    shopOpeningDate = it.shopOpeningDate,
                    stockValue = it.stockValue.toString(),
                    stockTakenDate = it.stockTakenDate
                )
                _isLoaded.value = true
            }

            // Load live investor list for this shop
            launch {
                investorRepo.getInvestorsForShop(shopId).collect { list ->
                    _shopInvestors.value = list
                }
            }
        } catch (e: Exception) {
            _error.value = "Failed to load shop: ${e.localizedMessage}"
        }
    }

    fun updateShopName(name: String) {
        _formState.update { it.copy(shopName = name) }
        _error.value = null
    }

    fun updateShopAddress(address: String) {
        _formState.update { it.copy(shopAddress = address) }
        _error.value = null
    }

    fun updateShopId(shopId: String) {
        _formState.update { it.copy(shopId = shopId) }
        _error.value = null
    }

    fun updateShopType(type: ShopType) {
        _formState.update { it.copy(shopType = type) }
        _error.value = null
    }

    fun updateZakathStatus(status: ZakathStatus) {
        _formState.update { it.copy(zakathStatus = status) }
        _error.value = null
    }

    fun updateLicenseExpiryDate(dateInMillis: Long) {
        _formState.update { it.copy(licenseExpiryDate = dateInMillis) }
        _error.value = null
    }

    fun updateShopOpeningDate(dateInMillis: Long) {
        _formState.update { it.copy(shopOpeningDate = dateInMillis) }
        _error.value = null
    }

    fun updateStockValue(value: String) {
        // Only allow numbers and decimal point
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(stockValue = value) }
            _error.value = null
        }
    }

    fun updateStockTakenDate(dateInMillis: Long) {
        _formState.update { it.copy(stockTakenDate = dateInMillis) }
        _error.value = null
    }

    fun saveShop(
        context: Context,
        onSuccess: () -> Unit = {},
        onFail: (String) -> Unit = {}
    ) = viewModelScope.launch {
        val state = _formState.value

        try {
            val db = AppDatabase.getDatabase(context)
            val repo = ShopRepository(db.shopDao())

            val shop = ShopInfo(
                id = editingShopId ?: 0,
                shopName = state.shopName,
                shopAddress = state.shopAddress,
                shopId = state.shopId,
                shopType = state.shopType?.name ?: "",
                zakathStatus = state.zakathStatus?.name ?: "",
                licenseExpiryDate = state.licenseExpiryDate ?: 0L,
                shopOpeningDate = state.shopOpeningDate ?: 0L,
                stockValue = state.stockValue.toDoubleOrNull() ?: 0.0,
                stockTakenDate = state.stockTakenDate ?: 0L
            )

            if (editingShopId != null) {
                repo.updateShop(shop)
            } else {
                repo.insertShop(shop)
            }
            _isSaved.value = true
            onSuccess()
        } catch (e: Exception) {
            onFail("Failed to save shop: ${e.localizedMessage}")
        }
    }
}