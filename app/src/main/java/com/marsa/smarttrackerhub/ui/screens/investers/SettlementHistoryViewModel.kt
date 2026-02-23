package com.marsa.smarttrackerhub.ui.screens.investers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.dao.SettlementEntryWithName
import com.marsa.smarttrackerhub.data.entity.YearEndSettlement
import com.marsa.smarttrackerhub.data.repository.YearEndSettlementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Loads the settlement history for a shop:
 *  - List of YearEndSettlement records (most recent first)
 *  - When the user expands a settlement → loads its per-investor entries
 *  - "Mark as Paid" dialog for each entry that still has an outstanding balance
 *
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
data class SettlementHistoryUiState(
    val shopName: String = "",
    val settlements: List<YearEndSettlement> = emptyList(),
    /** Entries for the currently expanded settlement (null = none expanded). */
    val expandedSettlementId: Int? = null,
    val expandedEntries: List<SettlementEntryWithName> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingEntries: Boolean = false,
    /** Non-null when the "Mark as Paid" dialog is open for this entry. */
    val dialogEntry: SettlementEntryWithName? = null,
    /** Non-null when the "Reverse/Delete Settlement" confirm dialog is open. */
    val deletingSettlement: YearEndSettlement? = null,
    val isDeleting: Boolean = false,
    val error: String? = null
)

class SettlementHistoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettlementHistoryUiState())
    val uiState: StateFlow<SettlementHistoryUiState> = _uiState

    private lateinit var settlementRepo: YearEndSettlementRepository
    private lateinit var db: AppDatabase

    private var initialized = false
    private var currentShopId: Int = 0

    fun init(context: Context, shopId: Int) {
        if (initialized) return
        initialized = true

        currentShopId = shopId
        db = AppDatabase.getDatabase(context)
        settlementRepo = YearEndSettlementRepository(db.yearEndSettlementDao())

        viewModelScope.launch {
            val shop = db.shopDao().getShopById(shopId)
            _uiState.value = _uiState.value.copy(
                shopName = shop?.shopName ?: "Shop #$shopId"
            )

            // Observe settlement list live
            settlementRepo.getSettlementsForShop(shopId).collect { list ->
                _uiState.value = _uiState.value.copy(
                    settlements = list,
                    isLoading = false
                )
            }
        }
    }

    /** Toggles expansion of a settlement row to show per-investor entries. */
    fun toggleSettlement(settlementId: Int) {
        val current = _uiState.value.expandedSettlementId
        if (current == settlementId) {
            // Collapse
            _uiState.value = _uiState.value.copy(
                expandedSettlementId = null,
                expandedEntries = emptyList()
            )
        } else {
            // Expand — load entries
            _uiState.value = _uiState.value.copy(
                expandedSettlementId = settlementId,
                expandedEntries = emptyList(),
                isLoadingEntries = true
            )
            viewModelScope.launch {
                settlementRepo.getSettlementEntries(settlementId).collect { entries ->
                    _uiState.value = _uiState.value.copy(
                        expandedEntries = entries,
                        isLoadingEntries = false
                    )
                }
            }
        }
    }

    // ── Mark as Paid dialog ────────────────────────────────────────────────

    /** Opens the "Mark as Paid" dialog for [entry]. */
    fun showMarkPaidDialog(entry: SettlementEntryWithName) {
        _uiState.value = _uiState.value.copy(dialogEntry = entry)
    }

    /** Closes the "Mark as Paid" dialog without saving. */
    fun dismissMarkPaidDialog() {
        _uiState.value = _uiState.value.copy(dialogEntry = null)
    }

    // ── Reverse / Delete Settlement ────────────────────────────────────────

    /** Opens the confirmation dialog for reversing/deleting a settlement. */
    fun showDeleteSettlementDialog(settlement: YearEndSettlement) {
        _uiState.value = _uiState.value.copy(deletingSettlement = settlement)
    }

    /** Dismisses the reversal confirmation without deleting. */
    fun dismissDeleteSettlementDialog() {
        _uiState.value = _uiState.value.copy(deletingSettlement = null)
    }

    /**
     * Permanently deletes the settlement and all its entries.
     * This effectively "reverses" the settlement, allowing it to be recalculated.
     */
    fun confirmDeleteSettlement() {
        val settlement = _uiState.value.deletingSettlement ?: return
        _uiState.value = _uiState.value.copy(isDeleting = true)
        viewModelScope.launch {
            try {
                settlementRepo.deleteSettlement(settlement.id)
                _uiState.value = _uiState.value.copy(
                    deletingSettlement = null,
                    isDeleting = false,
                    // Collapse if the deleted settlement was expanded
                    expandedSettlementId = if (_uiState.value.expandedSettlementId == settlement.id)
                        null else _uiState.value.expandedSettlementId,
                    expandedEntries = if (_uiState.value.expandedSettlementId == settlement.id)
                        emptyList() else _uiState.value.expandedEntries
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete settlement: ${e.localizedMessage}",
                    deletingSettlement = null,
                    isDeleting = false
                )
            }
        }
    }

    // ── Export / Report ────────────────────────────────────────────────────

    /**
     * Builds the full settlement report (all settlements + all their entries)
     * and shares it via the system share sheet.
     */
    fun exportReport(context: Context) {
        viewModelScope.launch {
            val settlements = _uiState.value.settlements
            val entriesBySettlement = mutableMapOf<Int, List<SettlementEntryWithName>>()

            for (s in settlements) {
                val entries = settlementRepo.getSettlementEntries(s.id).first()
                entriesBySettlement[s.id] = entries
            }

            InvestorReportExporter.shareSettlementReport(
                context = context,
                shopName = _uiState.value.shopName,
                settlements = settlements,
                entriesBySettlement = entriesBySettlement
            )
        }
    }

    /**
     * Persists the settlement payment and refreshes the expanded entries list.
     *
     * @param entry      the entry being settled
     * @param paidAmount the amount actually transferred
     * @param paidDate   epoch-millis of the transfer date
     */
    fun markEntrySettled(
        entry: SettlementEntryWithName,
        paidAmount: Double,
        paidDate: Long
    ) {
        viewModelScope.launch {
            try {
                settlementRepo.markEntrySettled(entry, paidAmount, paidDate)
                _uiState.value = _uiState.value.copy(dialogEntry = null)
                // Refresh the expanded entries so the UI reflects the change immediately
                val settlementId = entry.settlementId
                settlementRepo.getSettlementEntries(settlementId).collect { entries ->
                    _uiState.value = _uiState.value.copy(expandedEntries = entries)
                    return@collect
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to record payment: ${e.localizedMessage}",
                    dialogEntry = null
                )
            }
        }
    }
}
