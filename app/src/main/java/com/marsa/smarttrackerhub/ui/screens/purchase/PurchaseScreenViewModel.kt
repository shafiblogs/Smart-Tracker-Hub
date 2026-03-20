package com.marsa.smarttrackerhub.ui.screens.purchase

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
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
 * Reads the `purchaseBreakdown` array from the existing SmartTracker
 * summary/{shopId}/months/{monthYear} Firestore document.
 *
 * No Room caching — purchase breakdown is kept in an in-memory map
 * per session (simple and sufficient for this read-only screen).
 *
 * Created by Muhammed Shafi on 13/03/2026.
 * Moro Hub
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

    // In-memory cache: monthId → category-wise purchase list
    private val _purchaseCache = MutableStateFlow<Map<String, List<PurchaseItem>>>(emptyMap())
    val purchaseCache: StateFlow<Map<String, List<PurchaseItem>>> = _purchaseCache

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
            _shops.value = withContext(Dispatchers.IO) {
                getHomeShopUser(userAccessCode, database)
            }
        }
    }

    fun setSelectedShop(shop: ShopListDto?) {
        monthsListenerRegistration?.remove()
        _selectedShop.value = shop
        _selectedMonthId.value = null
        _availableMonths.value = emptyList()
        _purchaseCache.value = emptyMap()
        shop?.shopId?.let { shopId ->
            viewModelScope.launch {
                if (ensureAuth()) loadMonthListForShop(shopId)
            }
        }
    }

    private suspend fun ensureAuth(): Boolean {
        val auth = FirebaseAuth.getInstance(firebaseApp)
        return suspendCoroutine { cont ->
            auth.signInAnonymously()
                .addOnSuccessListener { cont.resume(true) }
                .addOnFailureListener { e ->
                    Log.e("PurchaseViewModel", "SmartTrackerApp sign-in failed: ${e.message}")
                    cont.resume(false)
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
        // Remove from cache to force a fresh Firestore fetch
        _purchaseCache.value = _purchaseCache.value.toMutableMap().apply { remove(monthId) }
        loadPurchasesForMonth(shopId, monthId)
        Log.d("PurchaseViewModel", "Refreshing purchase data for month $monthId")
    }

    // ─────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────

    private fun loadMonthListForShop(shopId: String) {
        monthsListenerRegistration = firestore
            .collection("summary")
            .document(shopId)
            .collection("months")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PurchaseViewModel", "Error fetching months for $shopId", error)
                    return@addSnapshotListener
                }
                val months = snapshot?.documents
                    ?.map { doc ->
                        MonthItem(
                            id = doc.id,
                            displayName = doc.id,
                            timestamp = TargetSaleCalculator.parseMonthYearToTimestamp(doc.id)
                        )
                    }
                    .orEmpty()
                    .sortedByDescending { it.timestamp }
                _availableMonths.value = months
                Log.d("PurchaseViewModel", "Loaded ${months.size} months for $shopId")
            }
    }

    private fun loadPurchasesForMonth(shopId: String, monthId: String) {
        _isLoadingMonth.value = true
        firestore
            .collection("summary")
            .document(shopId)
            .collection("months")
            .document(monthId)
            .get()
            .addOnSuccessListener { document ->
                @Suppress("UNCHECKED_CAST")
                val rawBreakdown =
                    document.get("purchaseBreakdown") as? List<Map<String, Any>> ?: emptyList()

                val items = rawBreakdown.map { map ->
                    PurchaseItem(
                        // Firestore deserialises integers as Long — convert to Int
                        categoryId = (map["categoryId"] as? Long)?.toInt() ?: 0,
                        categoryName = map["categoryName"] as? String ?: "",
                        totalAmount = (map["totalAmount"] as? Double)
                            ?: (map["totalAmount"] as? Long)?.toDouble() ?: 0.0
                    )
                }

                _purchaseCache.value = _purchaseCache.value.toMutableMap().apply {
                    put(monthId, items)
                }
                _isLoadingMonth.value = false
                Log.d(
                    "PurchaseViewModel",
                    "Loaded ${items.size} purchase categories for $shopId - $monthId"
                )
            }
            .addOnFailureListener { error ->
                Log.e("PurchaseViewModel", "Error fetching purchases for $monthId", error)
                _isLoadingMonth.value = false
            }
    }

    override fun onCleared() {
        super.onCleared()
        monthsListenerRegistration?.remove()
    }
}
