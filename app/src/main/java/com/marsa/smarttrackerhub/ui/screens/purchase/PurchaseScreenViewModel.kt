package com.marsa.smarttrackerhub.ui.screens.purchase

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.PurchaseEntity
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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Reads category-wise purchase breakdown from SmartTracker's
 * `summary/{shopId}/months/{monthId}` Firestore document (`purchaseBreakdown` field).
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
    private val purchaseDao = database.purchaseDao()
    private var monthsListenerRegistration: ListenerRegistration? = null

    fun loadScreenData(userAccessCode: AccessCode) {
        viewModelScope.launch {
            // Authenticate with SmartTracker Firebase before reading shops/{shopId}/months/
            val signedIn = suspendCoroutine<Boolean> { cont ->
                FirebaseAuth.getInstance(firebaseApp).signInAnonymously()
                    .addOnSuccessListener { cont.resume(true) }
                    .addOnFailureListener { e ->
                        Log.e("PurchaseViewModel", "SmartTracker auth failed: ${e.message}")
                        cont.resume(false)
                    }
            }
            if (!signedIn) return@launch

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
        // Only clear cache and re-register listener if shop actually changed
        val shopIdChanged = _selectedShop.value?.shopId != shop?.shopId

        if (shopIdChanged) {
            monthsListenerRegistration?.remove()
            _selectedMonthId.value = null
            _availableMonths.value = emptyList()
            _purchaseCache.value = emptyMap()
            _lastUpdatedCache.value = emptyMap()
        }

        _selectedShop.value = shop

        if (shopIdChanged) {
            shop?.shopId?.let { shopId ->
                loadMonthListForShop(shopId)
            }
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
        // Delete old data from Room to force fresh Firestore fetch (prevents duplicates)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                purchaseDao.deletePurchasesForMonth(shopId, monthId)
                loadPurchasesForMonth(shopId, monthId)
            } catch (e: Exception) {
                Log.e("PurchaseViewModel", "Error deleting old purchase data on refresh", e)
                loadPurchasesForMonth(shopId, monthId)
            }
        }
    }

    private fun loadMonthListForShop(shopId: String) {
        monthsListenerRegistration = firestore
            .collection("summary").document(shopId).collection("months")
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
     * Reads purchase breakdown for a month.
     * First tries to read from Room cache, then falls back to Firestore if not cached.
     */
    @Suppress("UNCHECKED_CAST")
    private fun loadPurchasesForMonth(shopId: String, monthId: String) {
        _isLoadingMonth.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // First, try to read from Room cache
                val cachedPurchases = purchaseDao.getPurchasesForMonth(shopId, monthId)
                if (cachedPurchases.isNotEmpty()) {
                    val items = cachedPurchases.map { entity: PurchaseEntity ->
                        PurchaseItem(
                            categoryId = entity.categoryId,
                            categoryName = entity.categoryName,
                            totalAmount = entity.totalAmount
                        )
                    }.sortedByDescending { item: PurchaseItem -> item.totalAmount }

                    val now = System.currentTimeMillis()
                    _purchaseCache.value = _purchaseCache.value.toMutableMap().apply { put(monthId, items) }
                    _lastUpdatedCache.value = _lastUpdatedCache.value.toMutableMap().apply { put(monthId, now) }
                    _isLoadingMonth.value = false
                    Log.d("PurchaseViewModel", "Loaded ${items.size} purchase categories from Room for $shopId $monthId")
                    return@launch
                }
            } catch (e: Exception) {
                Log.e("PurchaseViewModel", "Error reading from Room cache: ${e.message}")
            }

            // Fallback to Firestore if not in Room
            firestore
                .collection("summary").document(shopId)
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
                    Log.d("PurchaseViewModel", "Loaded ${items.size} purchase categories from Firestore for $shopId $monthId")
                }
                .addOnFailureListener { e ->
                    Log.e("PurchaseViewModel", "Error fetching purchases from Firestore $shopId $monthId", e)
                    _isLoadingMonth.value = false
                }
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
