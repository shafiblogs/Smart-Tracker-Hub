package com.marsa.smarttrackerhub.ui.screens.investers

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.InvestmentTransaction
import com.marsa.smarttrackerhub.data.repository.FirebaseSyncRepository
import com.marsa.smarttrackerhub.data.repository.InvestmentTransactionRepository
import com.marsa.smarttrackerhub.data.repository.ShopInvestorRepository
import com.marsa.smarttrackerhub.data.repository.ShopRepository
import com.marsa.smarttrackerhub.domain.PhaseTransactionDetail
import com.marsa.smarttrackerhub.domain.ShopInvestorSummary
import kotlinx.coroutines.Dispatchers
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
    val shopStatus: String = "Initial",
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
    private var shopRepo: ShopRepository? = null
    private var database: AppDatabase? = null
    private var currentShopId: Int = 0

    fun init(context: Context, shopId: Int) {
        currentShopId = shopId
        val db = AppDatabase.getDatabase(context)
        database = db
        shopRepo = ShopRepository(db.shopDao())
        shopInvestorRepo = ShopInvestorRepository(db.shopInvestorDao())
        txRepo = InvestmentTransactionRepository(db.investmentTransactionDao())

        viewModelScope.launch {
            try {
                val shop = shopRepo?.getShopById(shopId)
                _uiState.value = _uiState.value.copy(
                    shopName = shop?.shopName ?: "",
                    shopStatus = shop?.shopStatus ?: "Initial"
                )
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
                        shopStatus = _uiState.value.shopStatus,
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
                val updated = raw.copy(status = "Withdrawn", isSynced = false)
                repo.updateShopInvestor(updated)
                syncShopInvestor(updated)
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
                // Load the existing row so we keep its Firebase IDs (PhaseTransactionDetail
                // doesn't carry them). Editing must update the SAME Firestore doc, not orphan it.
                val existing = database?.investmentTransactionDao()?.getTransactionById(tx.transactionId)
                val updated = (existing ?: InvestmentTransaction(
                    id = tx.transactionId,
                    shopInvestorId = tx.shopInvestorId,
                    amount = amount,
                    transactionDate = tx.transactionDate,
                    phase = phase,
                    note = ""
                )).copy(
                    amount = amount,
                    phase = phase,
                    note = _uiState.value.editTxNote.trim(),
                    isSynced = false
                )
                txRepo?.updateTransaction(updated)
                syncTransaction(updated)
                refreshTotalInvested()
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
                // Record a deletion tombstone (BEFORE deleting) so the delete reliably
                // propagates to other devices on the next sync; then hard-delete locally.
                val existing = database?.investmentTransactionDao()?.getTransactionById(tx.transactionId)
                val fbId = existing?.transactionFirebaseId
                if (!fbId.isNullOrBlank()) {
                    database?.tombstoneDao()?.insert(
                        com.marsa.smarttrackerhub.data.entity.Tombstone(
                            collection = "transactions", firebaseId = fbId,
                            deletedAt = System.currentTimeMillis()
                        )
                    )
                }
                txRepo?.deleteTransactionById(tx.transactionId)
                refreshTotalInvested()
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

    /** Recalculates the total invested sum, persists it in shop_info, and re-pushes the shop. */
    private suspend fun refreshTotalInvested() {
        val tx = txRepo ?: return
        val sr = shopRepo ?: return
        val newTotal = tx.getTotalPaidForShop(currentShopId)
        sr.updateTotalInvested(currentShopId, newTotal)   // also resets shop.isSynced = 0
        // Push the updated total so other devices reflect it without a manual force-resync.
        database?.let { d ->
            viewModelScope.launch(Dispatchers.IO) {
                try { sr.getShopById(currentShopId)?.let { FirebaseSyncRepository(d).syncShop(it) } }
                catch (_: Exception) {}
            }
        }
    }

    /**
     * Validates and saves the new share % for the editing investor.
     *
     * Validation:
     * - New value must be > 0 and ≤ 100
     * - Adding this share on top of other ACTIVE investors must NOT exceed 100%
     *   (going below 100% is allowed — the user may be freeing up space for a new investor)
     */
    fun saveEditedShare() {
        val investor = _uiState.value.editingInvestor ?: return
        val newShare = _uiState.value.editShareInput.toDoubleOrNull()

        if (newShare == null || newShare <= 0.0 || newShare > 100.0) {
            _uiState.value = _uiState.value.copy(editShareError = "Enter a valid percentage (0–100)")
            return
        }

        // Only count OTHER ACTIVE investors — withdrawn shares are orphaned.
        val otherTotal = _uiState.value.investors
            .filter { it.shopInvestorId != investor.shopInvestorId && it.status == "Active" }
            .sumOf { it.sharePercentage }

        val newTotal = otherTotal + newShare
        // Only block if it would EXCEED 100% — going under is fine (frees room for new investors)
        if (newTotal > 100.0 + 0.01) {
            _uiState.value = _uiState.value.copy(
                editShareError = "Total would be ${String.format("%.1f", newTotal)}%. " +
                        "Other active investors hold ${String.format("%.1f", otherTotal)}%, " +
                        "so max allowed here is ${String.format("%.1f", 100.0 - otherTotal)}%."
            )
            return
        }

        _uiState.value = _uiState.value.copy(isSavingShare = true)
        viewModelScope.launch {
            try {
                val repo = shopInvestorRepo ?: return@launch
                val raw = repo.getShopInvestorById(investor.shopInvestorId) ?: return@launch
                val updated = raw.copy(sharePercentage = newShare, isSynced = false)
                repo.updateShopInvestor(updated)
                syncShopInvestor(updated)
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

    // ── Firebase push helpers (best-effort; SyncWorker retries on failure) ──────

    private fun syncShopInvestor(entity: com.marsa.smarttrackerhub.data.entity.ShopInvestor) {
        val db = database ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try { FirebaseSyncRepository(db).syncShopInvestor(entity) } catch (_: Exception) {}
        }
    }

    private fun syncTransaction(entity: InvestmentTransaction) {
        val db = database ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try { FirebaseSyncRepository(db).syncTransaction(entity) } catch (_: Exception) {}
        }
    }
}
