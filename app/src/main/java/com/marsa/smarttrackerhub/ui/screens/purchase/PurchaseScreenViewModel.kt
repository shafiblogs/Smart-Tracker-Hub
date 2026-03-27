package com.marsa.smarttrackerhub.ui.screens.purchase

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.domain.getHomeShopUser
import com.marsa.smarttrackerhub.ui.screens.sale.MonthItem
import com.marsa.smarttrackerhub.ui.screens.sale.TargetSaleCalculator
import com.marsa.smarttrackerhub.ui.screens.statement.ShopListDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reads category-wise purchase breakdown from SmartTracker's
 * `shops/{shopId}/months/{monthId}` Firestore document (`purchaseBreakdown` field).
 */
class PurchaseScreenViewModel(
    application: Application,
    private val firebaseApp: FirebaseApp
) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)

    private val _shops = MutableStateFlow<List<ShopListDto>>(emptyList())
    val shops: StateFlow<List<ShopListDto>> = _shops

    private val _availableMonths = MutableStateFlow<List<MonthItem>>(emptyList())
    val availableMonths: StateFlow<List<MonthItem>> = _availableMonths

    private val _purchaseCache = MutableStateFlow<Map<String, List<PurchaseItem>>>(emptyMap())
    val purchaseCache: StateFlow<Map<String, List<PurchaseItem>>> = _purchaseCache

    private val _lastUpdatedCache = MutableStateFlow<Map<String, Long>>(emptyMap())
    val lastUpdatedCache: StateFlow<Map<String, Long>> = _lastUpdatedCache

    private val _selectedShop = MutableStateFlow<ShopListDto?>(null)
    val selectedShop: StateFlow<ShopListDto?> = _selectedShop

    private val _selectedMonthId = MutableStateFlow<String?>(null)
    val selectedMonthId: StateFlow<String?> = _selectedMonthId

    private val _expanded = MutableStateFlow(false)
    val expanded: StateFlow<Boolean> = _expanded

    private val _isLoadingMonth = MutableStateFlow(false)
    val isLoadingMonth: StateFlow<Boolean> = _isLoadingMonth

    private val firestore = FirebaseFirestore.getInstance(firebaseApp)
    private var monthsListenerRegistration: ListenerRegistration? = null

    fun loadScreenData(userAccessCode: AccessCode) {
        viewModelScope.launch {
            val shops = withContext(Dispatchers.IO) {
                getHomeShopUser(userAccessCode, database)
            }
            _shops.value = shops
            if (_selectedShop.value == null) {
                shops.firstOrNull()?.let { setSelectedShop(it) }
            }
        }
    }

    fun setSelectedShop(shop: ShopListDto?) {
        monthsListenerRegistration?.remove()
        _selectedShop.value = shop
        _selectedMonthId.value = null
        _availableMonths.value = emptyList()
        _purchaseCache.value = emptyMap()
        _lastUpdatedCache.value = emptyMap()

        shop?.shopId?.let { shopId ->
            loadMonthListForShop(shopId)
        }
    }

    fun selectMonth(monthId: String) {
        _selectedMonthId.value = monthId
        val shopId = _selectedShop.value?.shopId ?: return
        if (!_purchaseCache.value.containsKey(monthId)) {
            loadPurchasesForMonth(shopId, monthId)
        }
    }

    fun setExpanded(value: Boolean) {
        _expanded.value = value
    }

    fun refreshMonth(monthId: String) {
        val shopId = _selectedShop.value?.shopId ?: return
        _purchaseCache.value = _purchaseCache.value.toMutableMap().apply { remove(monthId) }
        _lastUpdatedCache.value = _lastUpdatedCache.value.toMutableMap().apply { remove(monthId) }
        _isLoadingMonth.value = true
        loadPurchasesForMonth(shopId, monthId)
    }

    private fun loadMonthListForShop(shopId: String) {
        monthsListenerRegistration = firestore
            .collection("shops").document(shopId).collection("months")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PurchaseViewModel", "Error fetching months for $shopId", error)
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
                Log.d("PurchaseViewModel", "Loaded ${monthsList.size} months for $shopId")
            }
    }

    /**
     * Reads the `purchaseBreakdown` field from `summary/{shopId}/months/{monthId}`
     * and converts it to a sorted [PurchaseItem] list.
     */
    @Suppress("UNCHECKED_CAST")
    private fun loadPurchasesForMonth(shopId: String, monthId: String) {
        _isLoadingMonth.value = true
        firestore
            .collection("shops").document(shopId)
            .collection("months").document(monthId)
            .get()
            .addOnSuccessListener { document ->
                val breakdown = document.get("purchaseBreakdown") as? List<Map<String, Any>>
                val items = breakdown
                    ?.map { map ->
                        PurchaseItem(
                            categoryId   = (map["categoryId"] as? Long)?.toInt() ?: 0,
                            categoryName = (map["categoryName"] as? String) ?: "Uncategorised",
                            totalAmount  = parseAmount(map["totalAmount"])
                        )
                    }
                    ?.sortedByDescending { it.totalAmount }
                    ?: emptyList()

                val now = System.currentTimeMillis()
                _purchaseCache.value = _purchaseCache.value.toMutableMap().apply { put(monthId, items) }
                _lastUpdatedCache.value = _lastUpdatedCache.value.toMutableMap().apply { put(monthId, now) }
                _isLoadingMonth.value = false
                Log.d("PurchaseViewModel", "Loaded ${items.size} purchase categories for $shopId $monthId")
            }
            .addOnFailureListener { e ->
                Log.e("PurchaseViewModel", "Error fetching purchases $shopId $monthId", e)
                _isLoadingMonth.value = false
            }
    }

    private fun parseAmount(value: Any?): Double = when (value) {
        is Double -> value
        is Long   -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: 0.0
        else      -> 0.0
    }

    override fun onCleared() {
        super.onCleared()
        monthsListenerRegistration?.remove()
    }
}
