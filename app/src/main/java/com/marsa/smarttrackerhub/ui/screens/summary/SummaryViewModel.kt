package com.marsa.smarttrackerhub.ui.screens.summary

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.domain.AccountSummary
import com.marsa.smarttrackerhub.domain.getSummaryShopList
import com.marsa.smarttrackerhub.ui.screens.sale.TargetSaleCalculator
import com.marsa.smarttrackerhub.ui.screens.statement.ShopListDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SummaryViewModel(
    application: Application,
    private val firebaseApp: FirebaseApp
) : AndroidViewModel(application) {

    private val _shops = MutableStateFlow<List<ShopListDto>>(emptyList())
    val shops: StateFlow<List<ShopListDto>> = _shops

    private val _availableMonths = MutableStateFlow<List<MonthItem>>(emptyList())
    val availableMonths: StateFlow<List<MonthItem>> = _availableMonths

    private val _summariesCache = MutableStateFlow<Map<String, AccountSummary>>(emptyMap())
    val summariesCache: StateFlow<Map<String, AccountSummary>> = _summariesCache

    private val firestore = FirebaseFirestore.getInstance(firebaseApp)

    private val _selectedShop = MutableStateFlow<ShopListDto?>(null)
    val selectedShop: StateFlow<ShopListDto?> = _selectedShop

    private val _selectedMonthId = MutableStateFlow<String?>(null)
    val selectedMonthId: StateFlow<String?> = _selectedMonthId

    private val _expanded = MutableStateFlow(false)
    val expanded: StateFlow<Boolean> = _expanded

    private val _isLoadingMonth = MutableStateFlow(false)
    val isLoadingMonth: StateFlow<Boolean> = _isLoadingMonth

    private var monthsListenerRegistration: ListenerRegistration? = null

    fun loadScreenData(userAccessCode: AccessCode) {
        val shops = getSummaryShopList(userAccessCode)
        _shops.value = shops
        if (_selectedShop.value == null) {
            shops.firstOrNull()?.let { setSelectedShop(it) }
        }
    }

    fun setSelectedShop(shop: ShopListDto?) {
        monthsListenerRegistration?.remove()
        _selectedShop.value = shop
        _selectedMonthId.value = null
        _availableMonths.value = emptyList()
        _summariesCache.value = emptyMap()

        shop?.shopId?.let { shopId ->
            loadMonthListForShop(shopId)
        }
    }

    fun selectMonth(monthId: String) {
        _selectedMonthId.value = monthId
        val shopId = _selectedShop.value?.shopId ?: return
        if (!_summariesCache.value.containsKey(monthId)) {
            loadSummaryForMonth(shopId, monthId)
        }
    }

    fun setExpanded(value: Boolean) {
        _expanded.value = value
    }

    fun refreshMonth(monthId: String) {
        val shopId = _selectedShop.value?.shopId ?: return
        _summariesCache.value = _summariesCache.value.toMutableMap().apply { remove(monthId) }
        _isLoadingMonth.value = true
        loadSummaryForMonth(shopId, monthId)
    }

    private fun loadMonthListForShop(shopId: String) {
        monthsListenerRegistration = firestore
            .collection("summary").document(shopId).collection("months")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SummaryViewModel", "Error fetching months for $shopId", error)
                    return@addSnapshotListener
                }
                val monthsList = snapshot?.documents
                    ?.map { doc ->
                        MonthItem(
                            id = doc.id,
                            displayName = doc.id,
                            timestamp = TargetSaleCalculator.parseMonthYearToTimestamp(doc.id)
                        )
                    }
                    .orEmpty()
                    .sortedByDescending { it.timestamp }

                _availableMonths.value = monthsList
                if (_selectedMonthId.value == null && monthsList.isNotEmpty()) {
                    selectMonth(monthsList.first().id)
                }
                Log.d("SummaryViewModel", "Loaded ${monthsList.size} months for $shopId")
            }
    }

    /**
     * Reads `summary/{shopId}/months/{monthId}` from AccountTrackerApp Firebase
     * and deserializes directly to [AccountSummary].
     */
    private fun loadSummaryForMonth(shopId: String, monthId: String) {
        _isLoadingMonth.value = true
        firestore
            .collection("summary").document(shopId)
            .collection("months").document(monthId)
            .get()
            .addOnSuccessListener { document ->
                val summary = document.toObject(AccountSummary::class.java)
                    ?.let { if (it.lastUpdated == 0L) it.copy(lastUpdated = System.currentTimeMillis()) else it }
                if (summary != null) {
                    _summariesCache.value = _summariesCache.value.toMutableMap().apply {
                        put(monthId, summary)
                    }
                    Log.d("SummaryViewModel", "Loaded summary for $shopId $monthId")
                }
                _isLoadingMonth.value = false
            }
            .addOnFailureListener { e ->
                Log.e("SummaryViewModel", "Error fetching summary $shopId $monthId", e)
                _isLoadingMonth.value = false
            }
    }

    override fun onCleared() {
        super.onCleared()
        monthsListenerRegistration?.remove()
    }
}

data class MonthItem(
    val id: String,
    val displayName: String,
    val timestamp: Long = 0L
)
