package com.marsa.smarttrackerhub.ui.screens.investers

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.InvestmentTransaction
import com.marsa.smarttrackerhub.data.entity.InvestorInfo
import com.marsa.smarttrackerhub.data.entity.ShopInvestor
import com.marsa.smarttrackerhub.data.repository.InvestmentTransactionRepository
import com.marsa.smarttrackerhub.data.repository.InvestorRepository
import com.marsa.smarttrackerhub.data.repository.ShopInvestorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Records an investment payment (phase contribution) by an investor to a shop.
 *
 * Can be opened two ways:
 *  - From ShopInvestmentDashboard with shopId (user picks investor)
 *  - From InvestorDetailScreen with investorId + shopId (investor pre-filled)
 *
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

data class AddTransactionFormState(
    val selectedShopInvestorId: Int? = null,
    val selectedInvestorName: String = "",
    val amount: String = "",
    val phase: String = "",
    val transactionDate: Long = System.currentTimeMillis(),
    val note: String = "",
    val amountError: String? = null,
    val phaseError: String? = null,
    val investorError: String? = null
)

class AddTransactionViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _formState = MutableStateFlow(AddTransactionFormState())
    val formState: StateFlow<AddTransactionFormState> = _formState.asStateFlow()

    /** List of investors assigned to the shop — for the investor picker. */
    private val _shopInvestors = MutableStateFlow<List<Pair<ShopInvestor, InvestorInfo>>>(emptyList())
    val shopInvestors: StateFlow<List<Pair<ShopInvestor, InvestorInfo>>> = _shopInvestors.asStateFlow()

    /** Existing phase labels for this shop (for autocomplete suggestions). */
    private val _existingPhases = MutableStateFlow<List<String>>(emptyList())
    val existingPhases: StateFlow<List<String>> = _existingPhases.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val isFormValid: StateFlow<Boolean> = _formState
        .map {
            it.selectedShopInvestorId != null &&
                    it.amount.isNotBlank() &&
                    (it.amount.toDoubleOrNull() ?: 0.0) > 0.0 &&
                    it.phase.isNotBlank()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private lateinit var txRepo: InvestmentTransactionRepository
    private lateinit var shopInvestorRepo: ShopInvestorRepository
    private lateinit var investorRepo: InvestorRepository

    /**
     * @param shopId             Required — which shop this transaction is for
     * @param prefilledInvestorId > 0 → investor is pre-selected and locked
     */
    fun initDatabase(context: Context, shopId: Int, prefilledInvestorId: Int = 0) {
        val db = AppDatabase.getDatabase(context)
        txRepo = InvestmentTransactionRepository(db.investmentTransactionDao())
        shopInvestorRepo = ShopInvestorRepository(db.shopInvestorDao())
        investorRepo = InvestorRepository(db.investorDao())

        loadShopInvestors(shopId, prefilledInvestorId)
        loadExistingPhases(shopId)
    }

    private fun loadShopInvestors(shopId: Int, prefilledInvestorId: Int) = viewModelScope.launch {
        val rawList = shopInvestorRepo.getActiveInvestorsRaw(shopId)
        val pairs = rawList.mapNotNull { si ->
            val investor = investorRepo.getInvestorById(si.investorId) ?: return@mapNotNull null
            si to investor
        }
        _shopInvestors.value = pairs

        // Auto-select if prefilled
        if (prefilledInvestorId > 0) {
            pairs.find { it.second.id == prefilledInvestorId }?.let { (si, inv) ->
                selectInvestor(si.id, inv.investorName)
            }
        }
        // Auto-select if only one investor
        if (prefilledInvestorId == 0 && pairs.size == 1) {
            selectInvestor(pairs[0].first.id, pairs[0].second.investorName)
        }
    }

    private fun loadExistingPhases(shopId: Int) = viewModelScope.launch {
        txRepo.getTransactionsForShop(shopId).collect { txList ->
            _existingPhases.value = txList.map { it.phase }.distinct().sorted()
        }
    }

    fun selectInvestor(shopInvestorId: Int, investorName: String) {
        _formState.update { it.copy(selectedShopInvestorId = shopInvestorId, selectedInvestorName = investorName, investorError = null) }
    }

    fun updateAmount(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(amount = value, amountError = null) }
        }
    }

    fun updatePhase(value: String) {
        _formState.update { it.copy(phase = value, phaseError = null) }
    }

    fun updateDate(dateInMillis: Long) {
        _formState.update { it.copy(transactionDate = dateInMillis) }
    }

    fun updateNote(value: String) {
        _formState.update { it.copy(note = value) }
    }

    fun saveTransaction(
        context: Context,
        onSuccess: () -> Unit = {},
        onFail: (String) -> Unit = {}
    ) = viewModelScope.launch {
        val state = _formState.value

        val shopInvestorId = state.selectedShopInvestorId ?: run {
            _formState.update { it.copy(investorError = "Please select an investor") }
            return@launch
        }
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _formState.update { it.copy(amountError = "Enter a valid amount") }
            return@launch
        }
        if (state.phase.isBlank()) {
            _formState.update { it.copy(phaseError = "Enter a phase label (e.g. Phase 1)") }
            return@launch
        }

        try {
            txRepo.insertTransaction(
                InvestmentTransaction(
                    shopInvestorId = shopInvestorId,
                    amount = amount,
                    transactionDate = state.transactionDate,
                    phase = state.phase.trim(),
                    note = state.note.trim()
                )
            )
            _isSaved.value = true
            onSuccess()
        } catch (e: Exception) {
            onFail("Failed to save: ${e.localizedMessage}")
        }
    }
}
