package com.marsa.smarttrackerhub.ui.screens.investers

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.InvestorInfo
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import com.marsa.smarttrackerhub.data.entity.ShopInvestor
import com.marsa.smarttrackerhub.data.repository.InvestorRepository
import com.marsa.smarttrackerhub.data.repository.ShopInvestorRepository
import com.marsa.smarttrackerhub.data.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Assigns an investor to a shop with a fixed share %.
 * Actual money contributions are recorded separately via AddTransactionScreen.
 *
 * When the new investor's share would push the total over 100 %, the user
 * can pick WHICH existing investor donates the % (rather than forcing a
 * proportional dilution of everyone).
 *
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

/**
 * One row in the donor-selection list: an existing active investor plus
 * how much share they currently hold.
 */
data class DonorOption(
    val shopInvestorId: Int,
    val investorId: Int,
    val investorName: String,
    val currentShare: Double
)

/**
 * Preview item shown when adding the new investor requires redistributing
 * existing investors' shares.
 *
 * @param investorName display name
 * @param oldShare     share before the new investor is added
 * @param newShare     recalculated share after proportional dilution
 */
data class ShareRedistributionPreview(
    val investorName: String,
    val oldShare: Double,
    val newShare: Double
)

/**
 * How excess share (when total > 100 %) should be handled.
 */
enum class DistributionMode {
    /** Scale every existing active investor down proportionally (original behaviour). */
    PROPORTIONAL,
    /** Deduct the full new-investor share from one chosen investor. */
    SINGLE_DONOR
}

data class ShopInvestmentFormState(
    val selectedShopId: Int? = null,
    val selectedShopName: String = "",
    val selectedInvestorId: Int? = null,
    val selectedInvestorName: String = "",
    val sharePercentage: String = "",
    val shareError: String? = null,
    val shopError: String? = null,
    val investorError: String? = null,

    // ── Donor selection ──────────────────────────────────────────────────
    /** Active investors available as donors (populated when total > remaining). */
    val donorOptions: List<DonorOption> = emptyList(),
    /** Which distribution mode the user has chosen. */
    val distributionMode: DistributionMode = DistributionMode.PROPORTIONAL,
    /** The donor chosen when mode == SINGLE_DONOR. */
    val selectedDonorId: Int? = null,        // shopInvestorId
    val selectedDonorName: String = "",
    /** Error shown when chosen donor doesn't have enough share to give. */
    val donorError: String? = null
)

class AddShopInvestmentViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _formState = MutableStateFlow(ShopInvestmentFormState())
    val formState: StateFlow<ShopInvestmentFormState> = _formState.asStateFlow()

    private val _shops = MutableStateFlow<List<ShopInfo>>(emptyList())
    val shops: StateFlow<List<ShopInfo>> = _shops.asStateFlow()

    private val _investors = MutableStateFlow<List<InvestorInfo>>(emptyList())
    val investors: StateFlow<List<InvestorInfo>> = _investors.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** Remaining % still available to assign in the selected shop (informational). */
    private val _remainingPercentage = MutableStateFlow(100.0)
    val remainingPercentage: StateFlow<Double> = _remainingPercentage.asStateFlow()

    /** Sum of all active investors' shares for the selected shop. Used by Screen to detect overflow. */
    private val _existingTotal = MutableStateFlow(0.0)
    val existingTotal: StateFlow<Double> = _existingTotal.asStateFlow()

    /**
     * Non-empty when mode == PROPORTIONAL and adding the entered share % would
     * exceed 100 %. Shows old → new for every existing investor.
     */
    private val _redistributionPreview = MutableStateFlow<List<ShareRedistributionPreview>>(emptyList())
    val redistributionPreview: StateFlow<List<ShareRedistributionPreview>> = _redistributionPreview.asStateFlow()

    val isFormValid: StateFlow<Boolean> = _formState
        .map { s ->
            val shareOk = s.selectedShopId != null &&
                    s.selectedInvestorId != null &&
                    s.sharePercentage.isNotBlank() &&
                    (s.sharePercentage.toDoubleOrNull() ?: 0.0) > 0.0
            if (!shareOk) return@map false
            // When using single-donor mode a donor must be selected
            val needsDonor = s.donorOptions.isNotEmpty() &&
                    s.distributionMode == DistributionMode.SINGLE_DONOR
            if (needsDonor && s.selectedDonorId == null) return@map false
            true
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private lateinit var shopInvestorRepo: ShopInvestorRepository
    private lateinit var shopRepo: ShopRepository
    private lateinit var investorRepo: InvestorRepository

    /** Cache of investorId → name, loaded once per shop selection. */
    private val investorNameCache = mutableMapOf<Int, String>()

    /**
     * @param prefilledInvestorId > 0 → coming from InvestorDetailScreen (investor locked)
     * @param prefilledShopId     > 0 → coming from ShopInvestmentDashboard (shop locked)
     */
    fun initDatabase(context: Context, prefilledInvestorId: Int = 0, prefilledShopId: Int = 0) {
        val db = AppDatabase.getDatabase(context)
        shopInvestorRepo = ShopInvestorRepository(db.shopInvestorDao())
        shopRepo = ShopRepository(db.shopDao())
        investorRepo = InvestorRepository(db.investorDao())

        loadShops()
        loadInvestors()

        if (prefilledInvestorId > 0) {
            viewModelScope.launch {
                investorRepo.getInvestorById(prefilledInvestorId)
                    ?.let { selectInvestor(it.id, it.investorName) }
            }
        }
        if (prefilledShopId > 0) {
            viewModelScope.launch {
                shopRepo.getShopById(prefilledShopId)
                    ?.let { selectShop(it.id, it.shopName) }
            }
        }
    }

    private fun loadShops() = viewModelScope.launch {
        shopRepo.getAllShops().collect { _shops.value = it }
    }

    private fun loadInvestors() = viewModelScope.launch {
        investorRepo.getAllInvestors().collect { _investors.value = it }
    }

    fun selectShop(shopId: Int, shopName: String) {
        _formState.update { it.copy(selectedShopId = shopId, selectedShopName = shopName, shopError = null) }
        loadRemainingAndCache(shopId)
        recomputePreview()
    }

    fun selectInvestor(investorId: Int, investorName: String) {
        _formState.update { it.copy(selectedInvestorId = investorId, selectedInvestorName = investorName, investorError = null) }
    }

    fun selectDistributionMode(mode: DistributionMode) {
        _formState.update {
            it.copy(
                distributionMode = mode,
                selectedDonorId = null,
                selectedDonorName = "",
                donorError = null
            )
        }
        recomputePreview()
    }

    fun selectDonor(shopInvestorId: Int, donorName: String) {
        _formState.update {
            it.copy(
                selectedDonorId = shopInvestorId,
                selectedDonorName = donorName,
                donorError = null
            )
        }
        recomputePreview()
    }

    private fun loadRemainingAndCache(shopId: Int) = viewModelScope.launch {
        val existing = shopInvestorRepo.getActiveInvestorsRaw(shopId)
        val allocated = existing.sumOf { it.sharePercentage }
        _remainingPercentage.value = 100.0 - allocated
        _existingTotal.value = allocated

        // Build name cache and donor options
        investorNameCache.clear()
        val donors = mutableListOf<DonorOption>()
        existing.forEach { si ->
            val name = investorRepo.getInvestorById(si.investorId)?.investorName
                ?: "Investor #${si.investorId}"
            investorNameCache[si.investorId] = name
            donors.add(
                DonorOption(
                    shopInvestorId = si.id,
                    investorId = si.investorId,
                    investorName = name,
                    currentShare = si.sharePercentage
                )
            )
        }
        _formState.update { it.copy(donorOptions = donors) }
    }

    fun updateSharePercentage(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(sharePercentage = value, shareError = null, donorError = null) }
            recomputePreview()
        }
    }

    /**
     * Recalculates [redistributionPreview] whenever the shop, share %, mode, or
     * donor selection changes.
     * • PROPORTIONAL mode  → preview shows proportional dilution of all investors
     * • SINGLE_DONOR mode  → preview shows only the chosen donor's before/after
     */
    private fun recomputePreview() {
        val state = _formState.value
        val shopId = state.selectedShopId ?: run {
            _redistributionPreview.value = emptyList()
            return
        }
        val newShare = state.sharePercentage.toDoubleOrNull() ?: run {
            _redistributionPreview.value = emptyList()
            return
        }
        if (newShare <= 0.0 || newShare >= 100.0) {
            _redistributionPreview.value = emptyList()
            return
        }

        viewModelScope.launch {
            val existing = shopInvestorRepo.getActiveInvestorsRaw(shopId)
            val existingTotal = existing.sumOf { it.sharePercentage }

            if (existingTotal + newShare <= 100.0) {
                // Still within limit — no redistribution needed
                _redistributionPreview.value = emptyList()
                return@launch
            }

            when (state.distributionMode) {
                DistributionMode.PROPORTIONAL -> {
                    val scale = (100.0 - newShare) / existingTotal
                    _redistributionPreview.value = existing.map { si ->
                        ShareRedistributionPreview(
                            investorName = investorNameCache[si.investorId] ?: "Investor #${si.investorId}",
                            oldShare = si.sharePercentage,
                            newShare = si.sharePercentage * scale
                        )
                    }
                }
                DistributionMode.SINGLE_DONOR -> {
                    val donorId = state.selectedDonorId
                    if (donorId == null) {
                        _redistributionPreview.value = emptyList()
                        return@launch
                    }
                    val donor = existing.find { it.id == donorId }
                    if (donor == null) {
                        _redistributionPreview.value = emptyList()
                        return@launch
                    }
                    val excess = (existingTotal + newShare) - 100.0
                    _redistributionPreview.value = listOf(
                        ShareRedistributionPreview(
                            investorName = investorNameCache[donor.investorId] ?: "Investor #${donor.investorId}",
                            oldShare = donor.sharePercentage,
                            newShare = donor.sharePercentage - excess
                        )
                    )
                }
            }
        }
    }

    fun saveAssignment(
        context: Context,
        onSuccess: () -> Unit = {},
        onFail: (String) -> Unit = {}
    ) = viewModelScope.launch {
        val state = _formState.value

        val shopId = state.selectedShopId ?: run {
            _formState.update { it.copy(shopError = "Please select a shop") }
            return@launch
        }
        val investorId = state.selectedInvestorId ?: run {
            _formState.update { it.copy(investorError = "Please select an investor") }
            return@launch
        }
        val share = state.sharePercentage.toDoubleOrNull()
        if (share == null || share <= 0.0) {
            _formState.update { it.copy(shareError = "Enter a valid share percentage") }
            return@launch
        }
        if (share >= 100.0) {
            _formState.update { it.copy(shareError = "Share must be less than 100%") }
            return@launch
        }
        if (shopInvestorRepo.isInvestorInShop(shopId, investorId)) {
            onFail("This investor is already assigned to the selected shop")
            return@launch
        }

        try {
            val existing = shopInvestorRepo.getActiveInvestorsRaw(shopId)
            val existingTotal = existing.sumOf { it.sharePercentage }

            if (existingTotal + share > 100.0) {
                when (state.distributionMode) {

                    DistributionMode.PROPORTIONAL -> {
                        // Scale every existing investor proportionally
                        val scale = (100.0 - share) / existingTotal
                        existing.forEach { si ->
                            shopInvestorRepo.updateShopInvestor(
                                si.copy(sharePercentage = si.sharePercentage * scale)
                            )
                        }
                    }

                    DistributionMode.SINGLE_DONOR -> {
                        val donorSiId = state.selectedDonorId ?: run {
                            _formState.update { it.copy(donorError = "Please select which investor gives their share") }
                            return@launch
                        }
                        val donor = existing.find { it.id == donorSiId } ?: run {
                            _formState.update { it.copy(donorError = "Selected donor not found") }
                            return@launch
                        }
                        val excess = (existingTotal + share) - 100.0
                        val newDonorShare = donor.sharePercentage - excess
                        if (newDonorShare < 0.0) {
                            val donorName = investorNameCache[donor.investorId] ?: "Selected investor"
                            _formState.update {
                                it.copy(
                                    donorError = "$donorName only has ${String.format("%.1f", donor.sharePercentage)}% — " +
                                            "not enough to give ${String.format("%.1f", excess)}%"
                                )
                            }
                            return@launch
                        }
                        shopInvestorRepo.updateShopInvestor(
                            donor.copy(sharePercentage = newDonorShare)
                        )
                    }
                }
            }

            shopInvestorRepo.insertShopInvestor(
                ShopInvestor(
                    shopId = shopId,
                    investorId = investorId,
                    sharePercentage = share
                )
            )
            _isSaved.value = true
            onSuccess()
        } catch (e: Exception) {
            onFail("Failed to save: ${e.localizedMessage}")
        }
    }
}
