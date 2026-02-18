package com.marsa.smarttrackerhub.ui.screens.investers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.SettlementEntry
import com.marsa.smarttrackerhub.data.entity.YearEndSettlement
import com.marsa.smarttrackerhub.data.repository.InvestmentTransactionRepository
import com.marsa.smarttrackerhub.data.repository.ShopInvestorRepository
import com.marsa.smarttrackerhub.data.repository.YearEndSettlementRepository
import com.marsa.smarttrackerhub.domain.InvestorSettlementRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Computes year-end settlement rows for a shop:
 *  1. Loads all active ShopInvestors for the shop.
 *  2. For each investor → actualPaid = SUM of their transactions in this shop.
 *  3. totalInvested = SUM of ALL transactions for the shop.
 *  4. fairShare = sharePercentage / 100 × totalInvested
 *  5. balance = actualPaid - fairShare
 *       > 0 → investor overpaid → others owe them
 *       < 0 → investor underpaid → they owe others
 *
 * On confirm, persists YearEndSettlement + one SettlementEntry per investor.
 *
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
data class SettlementCalculatorUiState(
    val shopName: String = "",
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val totalInvested: Double = 0.0,
    val rows: List<InvestorSettlementRow> = emptyList(),
    val note: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

class SettlementCalculatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettlementCalculatorUiState())
    val uiState: StateFlow<SettlementCalculatorUiState> = _uiState

    private lateinit var db: AppDatabase
    private lateinit var shopInvestorRepo: ShopInvestorRepository
    private lateinit var transactionRepo: InvestmentTransactionRepository
    private lateinit var settlementRepo: YearEndSettlementRepository

    private var shopId: Int = 0
    private var initialized = false

    fun init(context: Context, shopId: Int) {
        if (initialized) return
        initialized = true

        this.shopId = shopId
        db = AppDatabase.getDatabase(context)
        shopInvestorRepo = ShopInvestorRepository(db.shopInvestorDao())
        transactionRepo = InvestmentTransactionRepository(db.investmentTransactionDao())
        settlementRepo = YearEndSettlementRepository(db.yearEndSettlementDao())

        viewModelScope.launch {
            val shop = db.shopDao().getShopById(shopId)
            _uiState.value = _uiState.value.copy(
                shopName = shop?.shopName ?: "Shop #$shopId"
            )
            calculateSettlement()
        }
    }

    /** Re-runs the calculation (e.g. after year change). */
    fun recalculate() {
        if (!initialized) return
        viewModelScope.launch { calculateSettlement() }
    }

    private suspend fun calculateSettlement() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        try {
            val activeInvestors = shopInvestorRepo.getActiveInvestorsRaw(shopId)
            val totalInvested = transactionRepo.getTotalPaidForShop(shopId)

            val rows = activeInvestors.map { si ->
                // Resolve investor name from DB
                val investor = db.investorDao().getInvestorById(si.investorId)
                val actualPaid = transactionRepo.getTotalPaidByInvestorForShop(shopId, si.investorId)
                val fairShare = (si.sharePercentage / 100.0) * totalInvested
                val balance = actualPaid - fairShare

                InvestorSettlementRow(
                    investorId = si.investorId,
                    investorName = investor?.investorName ?: "Investor #${si.investorId}",
                    sharePercentage = si.sharePercentage,
                    fairShareAmount = fairShare,
                    actualPaidAmount = actualPaid,
                    balanceAmount = balance
                )
            }.sortedByDescending { it.sharePercentage }

            _uiState.value = _uiState.value.copy(
                totalInvested = totalInvested,
                rows = rows,
                isLoading = false
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
        }
    }

    fun onNoteChange(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun onYearChange(year: Int) {
        _uiState.value = _uiState.value.copy(year = year)
    }

    /** Saves the computed settlement. Navigates back via saveSuccess flag. */
    fun confirmSettlement() {
        val state = _uiState.value
        if (state.rows.isEmpty() || state.isSaving) return
        _uiState.value = state.copy(isSaving = true, error = null)

        viewModelScope.launch {
            try {
                val settlement = YearEndSettlement(
                    shopId = shopId,
                    year = state.year,
                    totalInvested = state.totalInvested,
                    settlementDate = System.currentTimeMillis(),
                    note = state.note,
                    isCarriedForward = true
                )
                val entries = state.rows.map { row ->
                    SettlementEntry(
                        settlementId = 0, // will be replaced inside saveSettlement()
                        investorId = row.investorId,
                        fairShareAmount = row.fairShareAmount,
                        actualPaidAmount = row.actualPaidAmount,
                        balanceAmount = row.balanceAmount
                    )
                }
                settlementRepo.saveSettlement(settlement, entries)
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }
}
