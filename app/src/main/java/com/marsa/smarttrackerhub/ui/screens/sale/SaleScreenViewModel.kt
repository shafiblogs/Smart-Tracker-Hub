package com.marsa.smarttrackerhub.ui.screens.sale

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
import com.marsa.smarttrackerhub.data.entity.toDomain
import com.marsa.smarttrackerhub.data.entity.toEntity
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.domain.MonthlySummary
import com.marsa.smarttrackerhub.domain.getHomeShopUser
import com.marsa.smarttrackerhub.ui.screens.statement.ShopListDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SaleScreenViewModel(
    application: Application,
    private val firebaseApp: FirebaseApp
) : AndroidViewModel(application) {

    private val _shops = MutableStateFlow<List<ShopListDto>>(emptyList())
    val shops: StateFlow<List<ShopListDto>> = _shops

    private val _availableMonths = MutableStateFlow<List<MonthItem>>(emptyList())
    val availableMonths: StateFlow<List<MonthItem>> = _availableMonths

    private val _summariesCache = MutableStateFlow<Map<String, MonthlySummary>>(emptyMap())
    val summariesCache: StateFlow<Map<String, MonthlySummary>> = _summariesCache

    private val trackerFireStore = FirebaseFirestore.getInstance(firebaseApp)
    private val database = AppDatabase.getDatabase(application)
    private val summaryDao = database.summaryDao()
    private val purchaseDao = database.purchaseDao()

    private val _selectedShop = MutableStateFlow<ShopListDto?>(null)
    val selectedShop: StateFlow<ShopListDto?> = _selectedShop

    private val _selectedMonthId = MutableStateFlow<String?>(null)
    val selectedMonthId: StateFlow<String?> = _selectedMonthId

    private val _expanded = MutableStateFlow(false)
    val expanded: StateFlow<Boolean> = _expanded

    private val _isLoadingMonth = MutableStateFlow(false)
    val isLoadingMonth: StateFlow<Boolean> = _isLoadingMonth

    private var monthsListenerRegistration: ListenerRegistration? = null

    fun setSelectedShop(shop: ShopListDto?) {
        // Only clear cache and re-register listener if shop actually changed
        val shopIdChanged = _selectedShop.value?.shopId != shop?.shopId

        if (shopIdChanged) {
            monthsListenerRegistration?.remove()
            _selectedMonthId.value = null
            _availableMonths.value = emptyList()
            _summariesCache.value = emptyMap()
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
        if (!_summariesCache.value.containsKey(monthId)) {
            loadSummaryForMonth(shopId, monthId)
        }
    }

    fun setExpanded(value: Boolean) {
        _expanded.value = value
    }

    fun loadScreenData(userAccessCode: AccessCode) {
        viewModelScope.launch {
            // Authenticate with SmartTracker Firebase before reading shops/{shopId}/months/
            val signedIn = suspendCoroutine<Boolean> { cont ->
                FirebaseAuth.getInstance(firebaseApp).signInAnonymously()
                    .addOnSuccessListener { cont.resume(true) }
                    .addOnFailureListener { e ->
                        Log.e("SaleScreenViewModel", "SmartTracker auth failed: ${e.message}")
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

    fun refreshMonth(monthId: String) {
        val shopId = _selectedShop.value?.shopId ?: return
        _summariesCache.value = _summariesCache.value.toMutableMap().apply { remove(monthId) }
        _isLoadingMonth.value = true
        loadFromFirestore(shopId, monthId)
        Log.d("SaleScreenViewModel", "Refreshing month $monthId from server")
    }

    private fun loadMonthListForShop(shopId: String) {
        monthsListenerRegistration = trackerFireStore
            .collection("summary").document(shopId).collection("months")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SaleScreenViewModel", "Error fetching months for $shopId", error)
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
                Log.d("SaleScreenViewModel", "Loaded ${monthsList.size} month IDs for $shopId")
            }
    }

    private fun loadSummaryForMonth(shopId: String, monthId: String) {
        _isLoadingMonth.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cachedEntity = summaryDao.getSummary(shopId, monthId)
                if (cachedEntity != null) {
                    // Recalculate targets first so the minimum floor (1500) is always enforced,
                    // even for data that was cached before the floor rule was introduced.
                    recalculateTargetSales(shopId)
                    val refreshed = summaryDao.getSummary(shopId, monthId) ?: cachedEntity
                    _summariesCache.value = _summariesCache.value.toMutableMap().apply {
                        put(monthId, refreshed.toDomain())
                    }
                    _isLoadingMonth.value = false
                    Log.d("SaleScreenViewModel", "Loaded from cache: $shopId - $monthId")
                } else {
                    loadFromFirestore(shopId, monthId)
                }
            } catch (e: Exception) {
                Log.e("SaleScreenViewModel", "Error loading from cache", e)
                loadFromFirestore(shopId, monthId)
            }
        }
    }

    private fun loadFromFirestore(shopId: String, monthId: String) {
        trackerFireStore
            .collection("summary").document(shopId)
            .collection("months").document(monthId)
            .get()
            .addOnSuccessListener { document ->
                val summary = document.toObject(MonthlySummary::class.java)
                    ?.let { if (it.lastUpdated == 0L) it.copy(lastUpdated = System.currentTimeMillis()) else it }
                if (summary != null) {
                    _summariesCache.value = _summariesCache.value.toMutableMap().apply {
                        put(monthId, summary)
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            summaryDao.insertSummary(summary.toEntity(shopId, monthId))
                            recalculateTargetSales(shopId)
                            val updated = summaryDao.getSummary(shopId, monthId)
                            if (updated != null) {
                                _summariesCache.value = _summariesCache.value.toMutableMap().apply {
                                    put(monthId, updated.toDomain())
                                }
                            }

                            // Save purchase breakdown to Room
                            @Suppress("UNCHECKED_CAST")
                            val purchaseBreakdown = document.get("purchaseBreakdown") as? List<Map<String, Any>> ?: emptyList()
                            if (purchaseBreakdown.isNotEmpty()) {
                                val purchaseEntities = purchaseBreakdown.map { map ->
                                    PurchaseEntity(
                                        shopId = shopId,
                                        monthId = monthId,
                                        categoryId = (map["categoryId"] as? Long)?.toInt() ?: 0,
                                        categoryName = map["categoryName"] as? String ?: "Uncategorised",
                                        totalAmount = parseAmount(map["totalAmount"]),
                                        lastUpdated = System.currentTimeMillis()
                                    )
                                }
                                purchaseDao.insertPurchases(purchaseEntities)
                                Log.d("SaleScreenViewModel", "Saved ${purchaseEntities.size} purchase items for $shopId - $monthId")
                            }
                        } catch (e: Exception) {
                            Log.e("SaleScreenViewModel", "Error saving to local DB", e)
                        }
                    }
                    Log.d("SaleScreenViewModel", "Loaded from Firestore: $shopId - $monthId")
                }
                _isLoadingMonth.value = false
            }
            .addOnFailureListener { e ->
                Log.e("SaleScreenViewModel", "Error fetching month $monthId", e)
                _isLoadingMonth.value = false
            }
    }

    private fun parseAmount(value: Any?): Double = when (value) {
        is Double -> value
        is Long   -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: 0.0
        else      -> 0.0
    }

    private suspend fun recalculateTargetSales(shopId: String) {
        try {
            val allSummaries = summaryDao.getAllSummariesForShopAscending(shopId)
            if (allSummaries.isEmpty()) return
            val updated = TargetSaleCalculator.calculateTargetSalesForShop(allSummaries)
            summaryDao.insertSummaries(updated)
            updated.forEach { entity ->
                if (_summariesCache.value.containsKey(entity.monthId)) {
                    _summariesCache.value = _summariesCache.value.toMutableMap().apply {
                        put(entity.monthId, entity.toDomain())
                    }
                }
            }
            Log.d("SaleScreenViewModel", "Recalculated ${updated.size} target sales for $shopId")
        } catch (e: Exception) {
            Log.e("SaleScreenViewModel", "Error recalculating target sales", e)
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
