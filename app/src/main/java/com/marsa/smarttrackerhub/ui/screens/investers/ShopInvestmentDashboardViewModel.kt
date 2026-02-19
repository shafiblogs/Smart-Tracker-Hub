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
import kotlin.math.abs

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
    val error: String? = null,
    // Edit share dialog
    val editingInvestor: ShopInvestorSummary? = null,   // non-null = dialog open
    val editShareInput: String = "",                     // text field value
    val editShareError: String? = null,                  // validation error
    val isSavingShare: Boolean = false
)

class ShopInvestmentDashboardViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopInvestmentDashboardUiState())
    val uiState: StateFlow<ShopInvestmentDashboardUiState> = _uiState.asStateFlow()

    private var shopInvestorRepo: ShopInvestorRepository? = null
    private var currentShopId: Int = 0

    fun init(context: Context, shopId: Int) {
        currentShopId = shopId
        val db = AppDatabase.getDatabase(context)
        val shopRepo = ShopRepository(db.shopDao())
        shopInvestorRepo = ShopInvestorRepository(db.shopInvestorDao())
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
                    shopInvestorRepo!!.getInvestorsForShop(shopId),
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

    // ── Edit Share Dialog ─────────────────────────────────────────────────

    fun showEditShareDialog(investor: ShopInvestorSummary) {
        _uiState.value = _uiState.value.copy(
            editingInvestor = investor,
            editShareInput = String.format("%.2f", investor.sharePercentage),
            editShareError = null
        )
    }

    fun dismissEditShareDialog() {
        _uiState.value = _uiState.value.copy(
            editingInvestor = null,
            editShareInput = "",
            editShareError = null
        )
    }

    fun onEditShareInputChange(value: String) {
        _uiState.value = _uiState.value.copy(editShareInput = value, editShareError = null)
    }

    /**
     * Validates and saves the new share % for the editing investor.
     *
     * Validation:
     * - New value must be > 0
     * - Sum of all OTHER investors' shares + new value must equal exactly 100%
     *   (within a 0.01 tolerance to account for rounding)
     *
     * The user is expected to have already agreed on what the correct split is
     * (e.g. A sold 5% to C, so A goes 50→45, C goes 5→10).
     * This dialog edits ONE investor at a time — the caller must run it per investor.
     */
    fun saveEditedShare() {
        val investor = _uiState.value.editingInvestor ?: return
        val newShare = _uiState.value.editShareInput.toDoubleOrNull()

        if (newShare == null || newShare <= 0.0 || newShare > 100.0) {
            _uiState.value = _uiState.value.copy(editShareError = "Enter a valid percentage (0–100)")
            return
        }

        val otherTotal = _uiState.value.investors
            .filter { it.shopInvestorId != investor.shopInvestorId }
            .sumOf { it.sharePercentage }

        val newTotal = otherTotal + newShare
        if (abs(newTotal - 100.0) > 0.5) {
            val remaining = 100.0 - otherTotal
            _uiState.value = _uiState.value.copy(
                editShareError = "Total must equal 100%. " +
                        "Other investors hold ${String.format("%.2f", otherTotal)}%, " +
                        "so this investor's share must be ${String.format("%.2f", remaining)}%."
            )
            return
        }

        _uiState.value = _uiState.value.copy(isSavingShare = true)
        viewModelScope.launch {
            try {
                val repo = shopInvestorRepo ?: return@launch
                val raw = repo.getShopInvestorById(investor.shopInvestorId) ?: return@launch
                repo.updateShopInvestor(raw.copy(sharePercentage = newShare))
                _uiState.value = _uiState.value.copy(
                    editingInvestor = null,
                    editShareInput = "",
                    editShareError = null,
                    isSavingShare = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    editShareError = e.localizedMessage ?: "Failed to save",
                    isSavingShare = false
                )
            }
        }
    }
}
