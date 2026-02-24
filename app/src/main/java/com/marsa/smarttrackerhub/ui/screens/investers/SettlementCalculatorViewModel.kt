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
 * Computes a settlement for a shop covering the period from the last settlement
 * date (exclusive) up to the user-chosen [settlementDate] (inclusive).
 *
 * Period logic:
 *  - First settlement ever        → covers ALL transactions (fromDate = 0)
 *  - Subsequent settlements       → covers only transactions after the previous
 *                                   settlement's settlementDate
 *
 * Calculation per investor (within the period):
 *  1. actualPaid  = SUM of their transactions in this period
 *  2. totalInvested = SUM of ALL transactions in this period
 *  3. fairShare   = sharePercentage / 100 × totalInvested
 *  4. balance     = actualPaid - fairShare
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
    /** User-chosen end-of-period date (epoch millis). Defaults to today. */
    val settlementDate: Long = todayStartOfDay(),
    /** Start of the period (0 = all time for the very first settlement). */
    val periodStartDate: Long = 0L,
    val totalInvested: Double = 0.0,
    val rows: List<InvestorSettlementRow> = emptyList(),
    val note: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

private fun todayStartOfDay(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

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
            // Determine period start from the last settlement
            val lastSettlement = settlementRepo.getLatestSettlement(shopId)
            // Add one full day (86 400 000 ms) so the next period starts at the
            // beginning of the day after the last settlement date, not 1 ms later.
            val periodStart = if (lastSettlement != null) lastSettlement.settlementDate + 86_400_000L else 0L
            _uiState.value = _uiState.value.copy(periodStartDate = periodStart)
            calculateSettlement()
        }
    }

    /** Re-runs the calculation (called when user changes the settlement date). */
    fun recalculate() {
        if (!initialized) return
        viewModelScope.launch { calculateSettlement() }
    }

    private suspend fun calculateSettlement() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        try {
            val fromDate = _uiState.value.periodStartDate
            val settlementDate = _uiState.value.settlementDate
            // Only include investors who were active (joined) on or before the settlement date.
            // This ensures a new investor who joined in Year 3 does not appear in a Year 1–2 settlement.
            val activeInvestors = shopInvestorRepo.getActiveInvestorsAsOf(shopId, settlementDate)

            // Total invested in this period only
            val totalInvested = transactionRepo.getTotalPaidForShopSince(shopId, fromDate)

            val rows = activeInvestors.map { si ->
                val investor = db.investorDao().getInvestorById(si.investorId)
                // Amount this investor paid in this period only
                val actualPaid = transactionRepo.getTotalPaidByInvestorForShopSince(
                    shopId, si.investorId, fromDate
                )
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

    /** Called when user picks a new settlement date from the date picker. */
    fun onSettlementDateChange(dateMillis: Long) {
        _uiState.value = _uiState.value.copy(settlementDate = dateMillis)
        // No need to recalculate — the period start is fixed (based on last settlement).
        // The settlement date is just a label/cutoff stored with the record.
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
                    settlementDate = state.settlementDate,
                    periodStartDate = state.periodStartDate,
                    totalInvested = state.totalInvested,
                    note = state.note,
                    isCarriedForward = true
                )
                val entries = state.rows.map { row ->
                    SettlementEntry(
                        settlementId = 0, // replaced inside saveSettlement()
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
