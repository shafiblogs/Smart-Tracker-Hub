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
import kotlinx.coroutines.launch

/**
 * Loads the settlement history for a shop:
 *  - List of YearEndSettlement records (most recent first)
 *  - When the user expands a settlement → loads its per-investor entries
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
    val error: String? = null
)

class SettlementHistoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettlementHistoryUiState())
    val uiState: StateFlow<SettlementHistoryUiState> = _uiState

    private lateinit var settlementRepo: YearEndSettlementRepository
    private lateinit var db: AppDatabase

    private var initialized = false

    fun init(context: Context, shopId: Int) {
        if (initialized) return
        initialized = true

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
}
