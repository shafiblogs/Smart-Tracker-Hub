package com.marsa.smarttrackerhub.ui.screens.shops

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import com.marsa.smarttrackerhub.data.repository.ShopRepository
import com.marsa.smarttrackerhub.ui.screens.enums.ShopStatus
import com.marsa.smarttrackerhub.ui.screens.enums.ShopType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * Created by Muhammed Shafi on 12/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class AddShopViewModel : ViewModel() {
    private val _formState = MutableStateFlow(ShopFormState())
    val formState = _formState.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var editingAccountId: Int? = null

    val isFormValid: StateFlow<Boolean> = formState
        .map {
            it.shopName.isNotBlank() &&
                    it.shopCode.isNotBlank() &&
                    it.shopAddress.isNotBlank()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun updateShopName(name: String) {
        _formState.update { it.copy(shopName = name) }
        _error.value = null
    }

    fun updateShopAddress(name: String) {
        _formState.update { it.copy(shopAddress = name) }
        _error.value = null
    }

    fun updateShopCode(userCode: String) {
        _formState.update { it.copy(shopCode = userCode) }
        _error.value = null
    }

    fun updateShopType(name: ShopType) {
        _formState.update { it.copy(shopType = name) }
        _error.value = null
    }

    fun updateShopStatus(name: ShopStatus) {
        _formState.update { it.copy(shopStatus = name) }
        _error.value = null
    }


    fun saveAccount(
        context: Context,
        onSuccess: () -> Unit = {},
        onFail: (String) -> Unit = {}
    ) = viewModelScope.launch {
        val state = _formState.value

        try {
            val db = AppDatabase.getDatabase(context)
            val repo = ShopRepository(db.shopDao())

            val account = ShopInfo(
                id = editingAccountId ?: 0,
                shopName = state.shopName,
                shopAddress = state.shopAddress,
                shopCode = state.shopCode,
                shopType = state.shopType?.name ?: "",
                shopStatus = state.shopStatus?.name ?: ""
            )

            if (editingAccountId != null) {
                repo.updateShop(account)
            } else {
                repo.insertShop(account)
            }
            _isSaved.value = true
            onSuccess()
        } catch (e: Exception) {
            onFail("Failed to save shop: ${e.localizedMessage}")
        }
    }
}