package com.marsa.smarttrackerhub.ui.screens.sale

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.PurchaseEntity
import com.marsa.smarttrackerhub.data.entity.toDomain
import com.marsa.smarttrackerhub.data.entity.toEntity
import com.marsa.smarttrackerhub.domain.MonthlySummary
import com.marsa.smarttrackerhub.ui.components.percentChange
import com.marsa.smarttrackerhub.ui.screens.chart.PurchaseCategoryChartData
import com.marsa.smarttrackerhub.ui.screens.chart.PurchaseChartStatistics
import com.marsa.smarttrackerhub.ui.screens.purchase.PurchaseItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// Purchase budget floors — mirror HomeScreenViewModel so the detail's actual/target
// comparison matches the home screen exactly.
private const val MIN_PURCHASE_CATEGORY_TARGET = 500.0
private const val MIN_TOTAL_PURCHASE_TARGET = 10000.0

/**
 * Loads one shop's month list + a single month's Sales summary AND purchase breakdown from the
 * shared `summary/{shopId}/months/{monthId}` doc (Room-first, Firestore fallback). Powers the
 * Sales month-detail screen: month switcher, refresh, and the Sales + Purchase sections.
 */
class SalesDetailViewModel(
    application: Application,
    private val firebaseApp: FirebaseApp,
    private val shopId: String,
    initialMonthId: String
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val summaryDao = db.summaryDao()
    private val purchaseDao = db.purchaseDao()
    private val firestore = FirebaseFirestore.getInstance(firebaseApp)
    private var monthsListener: ListenerRegistration? = null

    private val _availableMonths = MutableStateFlow<List<MonthItem>>(emptyList())
    val availableMonths: StateFlow<List<MonthItem>> = _availableMonths

    private val _selectedMonthId = MutableStateFlow(initialMonthId)
    val selectedMonthId: StateFlow<String> = _selectedMonthId

    private val _summary = MutableStateFlow<MonthlySummary?>(null)
    val summary: StateFlow<MonthlySummary?> = _summary

    private val _purchaseItems = MutableStateFlow<List<PurchaseItem>>(emptyList())
    val purchaseItems: StateFlow<List<PurchaseItem>> = _purchaseItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /** Month-over-month comparison vs the next-older month (null until computed). */
    private val _comparison = MutableStateFlow<SalesComparison?>(null)
    val comparison: StateFlow<SalesComparison?> = _comparison

    /** Per-category purchase actual-vs-target (target = prev month × 1.10), like the home screen. */
    private val _purchaseChart = MutableStateFlow<List<PurchaseCategoryChartData>>(emptyList())
    val purchaseChart: StateFlow<List<PurchaseCategoryChartData>> = _purchaseChart

    private val _purchaseStats = MutableStateFlow<PurchaseChartStatistics?>(null)
    val purchaseStats: StateFlow<PurchaseChartStatistics?> = _purchaseStats

    init {
        viewModelScope.launch {
            val ok = suspendCoroutine<Boolean> { cont ->
                FirebaseAuth.getInstance(firebaseApp).signInAnonymously()
                    .addOnSuccessListener { cont.resume(true) }
                    .addOnFailureListener {
                        Log.e("SalesDetailVM", "auth failed: ${it.message}")
                        cont.resume(false)
                    }
            }
            if (ok) loadMonths()
            loadMonth(_selectedMonthId.value)
        }
    }

    private fun loadMonths() {
        monthsListener = firestore.collection("summary").document(shopId).collection("months")
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                _availableMonths.value = snap?.documents
                    ?.map { MonthItem(id = it.id, displayName = it.id,
                        timestamp = TargetSaleCalculator.parseMonthYearToTimestamp(it.id)) }
                    .orEmpty()
                    .sortedByDescending { it.timestamp }
                updateComparison()
                updatePurchaseChart()
            }
    }

    fun selectMonth(monthId: String) {
        if (monthId == _selectedMonthId.value && _summary.value != null) return
        _selectedMonthId.value = monthId
        loadMonth(monthId)
    }

    /** Older month (list is newest→oldest, so previous = index + 1). */
    fun goOlder() = move(1)

    /** Newer month. */
    fun goNewer() = move(-1)

    private fun move(delta: Int) {
        val months = _availableMonths.value
        val idx = months.indexOfFirst { it.id == _selectedMonthId.value }
        if (idx < 0) return
        (idx + delta).takeIf { it in months.indices }?.let { selectMonth(months[it].id) }
    }

    fun refresh() {
        val monthId = _selectedMonthId.value
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { purchaseDao.deletePurchasesForMonth(shopId, monthId) }
            loadFromFirestore(monthId)
        }
    }

    private fun loadMonth(monthId: String) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val cached = summaryDao.getSummary(shopId, monthId)
            if (cached != null) {
                recalcTargets()
                val fresh = summaryDao.getSummary(shopId, monthId) ?: cached
                _summary.value = fresh.toDomain()
                _purchaseItems.value = loadPurchasesRoom(monthId)
                _isLoading.value = false
                updateComparison()
                updatePurchaseChart()
            } else {
                loadFromFirestore(monthId)
            }
        }
    }

    private suspend fun loadPurchasesRoom(monthId: String): List<PurchaseItem> =
        purchaseDao.getPurchasesForMonth(shopId, monthId)
            .map { PurchaseItem(it.categoryId, it.categoryName, it.totalAmount) }
            .sortedByDescending { it.totalAmount }

    @Suppress("UNCHECKED_CAST")
    private fun loadFromFirestore(monthId: String) {
        firestore.collection("summary").document(shopId)
            .collection("months").document(monthId)
            .get()
            .addOnSuccessListener { doc ->
                val s = doc.toObject(MonthlySummary::class.java)
                    ?.let { if (it.lastUpdated == 0L) it.copy(lastUpdated = System.currentTimeMillis()) else it }
                if (s != null) {
                    _summary.value = s
                    updateComparison()
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            summaryDao.insertSummary(s.toEntity(shopId, monthId))
                            recalcTargets()
                            summaryDao.getSummary(shopId, monthId)?.let { _summary.value = it.toDomain() }

                            val breakdown = doc.get("purchaseBreakdown") as? List<Map<String, Any>> ?: emptyList()
                            val entities = breakdown.map { m ->
                                PurchaseEntity(
                                    shopId = shopId,
                                    monthId = monthId,
                                    categoryId = (m["categoryId"] as? Long)?.toInt() ?: 0,
                                    categoryName = (m["categoryName"] as? String) ?: "Uncategorised",
                                    totalAmount = parseAmount(m["totalAmount"]),
                                    lastUpdated = System.currentTimeMillis()
                                )
                            }
                            if (entities.isNotEmpty()) {
                                purchaseDao.deletePurchasesForMonth(shopId, monthId)
                                purchaseDao.insertPurchases(entities)
                            }
                            _purchaseItems.value = entities
                                .map { PurchaseItem(it.categoryId, it.categoryName, it.totalAmount) }
                                .sortedByDescending { it.totalAmount }
                            updatePurchaseChart()
                        } catch (e: Exception) {
                            Log.e("SalesDetailVM", "save error", e)
                        }
                    }
                }
                _isLoading.value = false
            }
            .addOnFailureListener {
                Log.e("SalesDetailVM", "load $monthId failed", it)
                _isLoading.value = false
            }
    }

    /** Recompute the vs-previous-month delta for the currently selected month. */
    private fun updateComparison() {
        val current = _summary.value
        val months = _availableMonths.value
        val idx = months.indexOfFirst { it.id == _selectedMonthId.value }
        val prev = months.getOrNull(idx + 1)
        if (current == null || idx < 0 || prev == null) {
            _comparison.value = null
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val prevSummary = fetchSummaryDomain(prev.id)
            _comparison.value = SalesComparison(
                referenceLabel = prev.displayName,
                totalSalesDeltaPct = prevSummary?.let { percentChange(current.totalSales, it.totalSales) }
            )
        }
    }

    /** Room-first, Firestore-fallback fetch of one month's summary (for comparison only). */
    private suspend fun fetchSummaryDomain(monthId: String): MonthlySummary? {
        summaryDao.getSummary(shopId, monthId)?.let { return it.toDomain() }
        return suspendCoroutine { cont ->
            firestore.collection("summary").document(shopId)
                .collection("months").document(monthId).get()
                .addOnSuccessListener { cont.resume(it.toObject(MonthlySummary::class.java)) }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    /**
     * Recompute per-category purchase actual-vs-target for the selected month.
     * Target = previous month's category amount × 1.10 (floored), matching the home screen.
     */
    private fun updatePurchaseChart() {
        val current = _purchaseItems.value
        if (current.isEmpty()) {
            _purchaseChart.value = emptyList()
            _purchaseStats.value = null
            return
        }
        val months = _availableMonths.value
        val idx = months.indexOfFirst { it.id == _selectedMonthId.value }
        val prevId = months.getOrNull(idx + 1)?.id
        viewModelScope.launch(Dispatchers.IO) {
            val prevByCat = (prevId?.let { fetchPurchaseItems(it) } ?: emptyList())
                .associate { it.categoryId to it.totalAmount }
            val chart = current.sortedByDescending { it.totalAmount }.map { item ->
                val prev = prevByCat[item.categoryId]
                PurchaseCategoryChartData(
                    categoryId = item.categoryId,
                    categoryName = item.categoryName,
                    actual = item.totalAmount,
                    target = if (prev != null && prev > 0)
                        maxOf(prev * 1.10, MIN_PURCHASE_CATEGORY_TARGET)
                    else
                        MIN_PURCHASE_CATEGORY_TARGET
                )
            }
            _purchaseChart.value = chart
            _purchaseStats.value = PurchaseChartStatistics(
                totalActual = chart.sumOf { it.actual },
                totalTarget = maxOf(chart.sumOf { it.target }, MIN_TOTAL_PURCHASE_TARGET),
                monthLabel = _selectedMonthId.value,
                categoriesOnTarget = chart.count { !it.hasTarget || it.actual >= it.target },
                totalCategories = chart.size
            )
        }
    }

    /** Room-first, Firestore-fallback fetch of one month's purchase breakdown. */
    private suspend fun fetchPurchaseItems(monthId: String): List<PurchaseItem> {
        val cached = purchaseDao.getPurchasesForMonth(shopId, monthId)
        if (cached.isNotEmpty()) {
            return cached.map { PurchaseItem(it.categoryId, it.categoryName, it.totalAmount) }
        }
        @Suppress("UNCHECKED_CAST")
        return suspendCoroutine { cont ->
            firestore.collection("summary").document(shopId)
                .collection("months").document(monthId).get()
                .addOnSuccessListener { doc ->
                    val raw = doc.get("purchaseBreakdown") as? List<Map<String, Any>> ?: emptyList()
                    cont.resume(raw.map { m ->
                        PurchaseItem(
                            categoryId = (m["categoryId"] as? Long)?.toInt() ?: 0,
                            categoryName = (m["categoryName"] as? String) ?: "Uncategorised",
                            totalAmount = parseAmount(m["totalAmount"])
                        )
                    })
                }
                .addOnFailureListener { cont.resume(emptyList()) }
        }
    }

    private suspend fun recalcTargets() {
        val all = summaryDao.getAllSummariesForShopAscending(shopId)
        if (all.isEmpty()) return
        summaryDao.insertSummaries(TargetSaleCalculator.calculateTargetSalesForShop(all))
    }

    private fun parseAmount(v: Any?): Double = when (v) {
        is Double -> v
        is Long -> v.toDouble()
        is String -> v.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    override fun onCleared() {
        super.onCleared()
        monthsListener?.remove()
    }
}

/** Sales month-over-month comparison (vs the next-older month). */
data class SalesComparison(
    val referenceLabel: String,
    val totalSalesDeltaPct: Double?
)

class SalesDetailViewModelFactory(
    private val application: Application,
    private val firebaseApp: FirebaseApp,
    private val shopId: String,
    private val monthId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SalesDetailViewModel::class.java)) {
            return SalesDetailViewModel(application, firebaseApp, shopId, monthId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
