package com.marsa.smarttrackerhub.ui.screens.investers

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.InvestmentTransaction
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
    val isSavingShare: Boolean = false,
    // Withdraw investor dialog
    val withdrawingInvestor: ShopInvestorSummary? = null,  // non-null = confirm dialog open
    val isWithdrawing: Boolean = false,
    // Edit/Delete transaction dialog
    val editingTransaction: PhaseTransactionDetail? = null, // non-null = dialog open
    val editTxAmount: String = "",
    val editTxPhase: String = "",
    val editTxNote: String = "",
    val editTxAmountError: String? = null,
    val editTxPhaseError: String? = null,
    val isSavingTransaction: Boolean = false
)

class ShopInvestmentDashboardViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopInvestmentDashboardUiState())
    val uiState: StateFlow<ShopInvestmentDashboardUiState> = _uiState.asStateFlow()

    private var shopInvestorRepo: ShopInvestorRepository? = null
    private var txRepo: InvestmentTransactionRepository? = null
    private var currentShopId: Int = 0

    fun init(context: Context, shopId: Int) {
        currentShopId = shopId
        val db = AppDatabase.getDatabase(context)
        val shopRepo = ShopRepository(db.shopDao())
        shopInvestorRepo = ShopInvestorRepository(db.shopInvestorDao())
        txRepo = InvestmentTransactionRepository(db.investmentTransactionDao())

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
                    txRepo!!.getTransactionsForShop(shopId)
                ) { investors, transactions ->
                    val totalCapital = transactions.sumOf { it.amount }
                    // Only count active investors' shares in the allocated % header
                    val allocatedPct = investors
                        .filter { it.status == "Active" }
                        .sumOf { it.sharePercentage }
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

    // ── Withdraw Investor Dialog ──────────────────────────────────────────

    fun showWithdrawDialog(investor: ShopInvestorSummary) {
        _uiState.value = _uiState.value.copy(withdrawingInvestor = investor)
    }

    fun dismissWithdrawDialog() {
        _uiState.value = _uiState.value.copy(withdrawingInvestor = null)
    }

    /**
     * Marks the investor's ShopInvestor record as "Withdrawn".
     * Their historical transactions are preserved; they just won't appear in new calculations.
     */
    fun confirmWithdrawInvestor() {
        val investor = _uiState.value.withdrawingInvestor ?: return
        _uiState.value = _uiState.value.copy(isWithdrawing = true)
        viewModelScope.launch {
            try {
                val repo = shopInvestorRepo ?: return@launch
                val raw = repo.getShopInvestorById(investor.shopInvestorId) ?: return@launch
                repo.updateShopInvestor(raw.copy(status = "Withdrawn"))
                _uiState.value = _uiState.value.copy(
                    withdrawingInvestor = null,
                    isWithdrawing = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.localizedMessage ?: "Failed to withdraw investor",
                    withdrawingInvestor = null,
                    isWithdrawing = false
                )
            }
        }
    }

    // ── Edit / Delete Transaction Dialog ──────────────────────────────────

    fun showEditTransactionDialog(tx: PhaseTransactionDetail) {
        _uiState.value = _uiState.value.copy(
            editingTransaction = tx,
            editTxAmount = String.format("%.2f", tx.amount),
            editTxPhase = tx.phase,
            editTxNote = tx.note,
            editTxAmountError = null,
            editTxPhaseError = null
        )
    }

    fun dismissEditTransactionDialog() {
        _uiState.value = _uiState.value.copy(
            editingTransaction = null,
            editTxAmount = "",
            editTxPhase = "",
            editTxNote = "",
            editTxAmountError = null,
            editTxPhaseError = null
        )
    }

    fun onEditTxAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(editTxAmount = value, editTxAmountError = null)
    }

    fun onEditTxPhaseChange(value: String) {
        _uiState.value = _uiState.value.copy(editTxPhase = value, editTxPhaseError = null)
    }

    fun onEditTxNoteChange(value: String) {
        _uiState.value = _uiState.value.copy(editTxNote = value)
    }

    fun saveEditedTransaction() {
        val tx = _uiState.value.editingTransaction ?: return
        val amount = _uiState.value.editTxAmount.toDoubleOrNull()
        val phase = _uiState.value.editTxPhase.trim()

        if (amount == null || amount <= 0.0) {
            _uiState.value = _uiState.value.copy(editTxAmountError = "Enter a valid amount")
            return
        }
        if (phase.isBlank()) {
            _uiState.value = _uiState.value.copy(editTxPhaseError = "Phase cannot be empty")
            return
        }

        _uiState.value = _uiState.value.copy(isSavingTransaction = true)
        viewModelScope.launch {
            try {
                txRepo?.updateTransaction(
                    InvestmentTransaction(
                        id = tx.transactionId,
                        shopInvestorId = tx.shopInvestorId,
                        amount = amount,
                        transactionDate = tx.transactionDate,
                        phase = phase,
                        note = _uiState.value.editTxNote.trim()
                    )
                )
                _uiState.value = _uiState.value.copy(
                    editingTransaction = null,
                    editTxAmount = "",
                    editTxPhase = "",
                    editTxNote = "",
                    editTxAmountError = null,
                    editTxPhaseError = null,
                    isSavingTransaction = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    editTxAmountError = e.localizedMessage ?: "Failed to save",
                    isSavingTransaction = false
                )
            }
        }
    }

    fun deleteTransaction() {
        val tx = _uiState.value.editingTransaction ?: return
        _uiState.value = _uiState.value.copy(isSavingTransaction = true)
        viewModelScope.launch {
            try {
                txRepo?.deleteTransactionById(tx.transactionId)
                _uiState.value = _uiState.value.copy(
                    editingTransaction = null,
                    editTxAmount = "",
                    editTxPhase = "",
                    editTxNote = "",
                    isSavingTransaction = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.localizedMessage ?: "Failed to delete",
                    isSavingTransaction = false,
                    editingTransaction = null
                )
            }
        }
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

        // Only count OTHER ACTIVE investors — withdrawn investors' shares are
        // orphaned and must not constrain the active total.
        val otherTotal = _uiState.value.investors
            .filter { it.shopInvestorId != investor.shopInvestorId && it.status == "Active" }
            .sumOf { it.sharePercentage }

        val newTotal = otherTotal + newShare
        if (abs(newTotal - 100.0) > 0.5) {
            val remaining = 100.0 - otherTotal
            _uiState.value = _uiState.value.copy(
                editShareError = "Active investors hold ${String.format("%.1f", otherTotal)}%, " +
                        "so this share must be ${String.format("%.1f", remaining)}%."
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
