package com.marsa.smarttrackerhub.ui.screens.investers

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.repository.FirebaseSyncRepository
import com.marsa.smarttrackerhub.data.repository.InvestorRepository
import com.marsa.smarttrackerhub.data.repository.ShopInvestorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class InvestorDetailViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvestorDetailUiState())
    val uiState: StateFlow<InvestorDetailUiState> = _uiState.asStateFlow()

    private var database: AppDatabase? = null
    private var investorRepo: InvestorRepository? = null
    private var shopInvestorRepo: ShopInvestorRepository? = null
    private var currentInvestorId: Int = 0

    fun init(context: Context, investorId: Int) {
        val db = AppDatabase.getDatabase(context)
        database = db
        currentInvestorId = investorId
        val investorRepo = InvestorRepository(db.investorDao())
        val shopInvestorRepo = ShopInvestorRepository(db.shopInvestorDao())
        this.investorRepo = investorRepo
        this.shopInvestorRepo = shopInvestorRepo

        // Load static investor info once
        viewModelScope.launch {
            try {
                val investor = investorRepo.getInvestorById(investorId)
                _uiState.update { it.copy(investor = investor) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }

        // Live shop summaries — updates whenever a transaction is added
        viewModelScope.launch {
            try {
                shopInvestorRepo.getShopsForInvestor(investorId).collect { summaries ->
                    val totalPaid = summaries.sumOf { it.totalPaid }
                    val activeCount = summaries.count { it.status == "Active" }
                    _uiState.update {
                        it.copy(
                            shopSummaries = summaries,
                            totalPaidAllShops = totalPaid,
                            activeShopCount = activeCount,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    // ── Delete investor ───────────────────────────────────────────────────────

    fun requestDelete() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun dismissBlockedMessage() {
        _uiState.update { it.copy(deleteBlockedMessage = null) }
    }

    /**
     * Deletes the investor — but only if they have no payment records. Otherwise the delete
     * is blocked with a message (the user must remove their payments first). On success the
     * investor doc (and any payment-free shop-investor link docs) are removed from Firestore.
     */
    fun confirmDelete(onDeleted: () -> Unit) {
        val db = database ?: return
        val investor = _uiState.value.investor ?: return
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            try {
                val totalPaid = db.shopInvestorDao().getTotalPaidByInvestor(currentInvestorId)
                if (totalPaid > 0.0) {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            showDeleteDialog = false,
                            deleteBlockedMessage =
                                "Cannot delete \"${investor.investorName}\" — they have payment " +
                                "records. Delete their payments (in each shop) first."
                        )
                    }
                    return@launch
                }

                // Gather payment-free link doc IDs before the local delete cascades them away.
                val linkFbIds = db.shopInvestorDao()
                    .getLinksForInvestor(currentInvestorId)
                    .map { it.shopInvestorFirebaseId }

                investorRepo?.deleteInvestor(investor)

                withContext(Dispatchers.IO) {
                    try {
                        FirebaseSyncRepository(db)
                            .deleteInvestorWithLinks(investor.investorId, linkFbIds)
                    } catch (_: Exception) {}
                }

                _uiState.update { it.copy(isDeleting = false, showDeleteDialog = false) }
                onDeleted()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        showDeleteDialog = false,
                        error = e.localizedMessage ?: "Failed to delete investor"
                    )
                }
            }
        }
    }
}
