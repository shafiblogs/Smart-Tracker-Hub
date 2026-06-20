package com.marsa.smarttrackerhub.ui.screens.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.SummaryEntity
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.domain.ChartStatistics
import com.marsa.smarttrackerhub.domain.MonthRange
import com.marsa.smarttrackerhub.domain.getHomeShopUser
import com.marsa.smarttrackerhub.ui.screens.chart.MonthlyChartData
import com.marsa.smarttrackerhub.ui.screens.chart.PurchaseCategoryChartData
import com.marsa.smarttrackerhub.ui.screens.chart.PurchaseChartStatistics
import com.marsa.smarttrackerhub.ui.screens.purchase.PurchaseItem
import com.marsa.smarttrackerhub.ui.screens.sale.TargetSaleCalculator
import com.marsa.smarttrackerhub.ui.screens.statement.ShopListDto
import com.marsa.smarttrackerhub.utils.getShortMonthName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val MIN_PURCHASE_CATEGORY_TARGET = 500.0
private const val MIN_TOTAL_PURCHASE_TARGET = 10000.0

class HomeScreenViewModel(
    application: Application,
    private val firebaseApp: FirebaseApp
) : ViewModel() {

    private val _selectedShop = MutableStateFlow<ShopListDto?>(null)
    val selectedShop: StateFlow<ShopListDto?> = _selectedShop

    private val _expanded = MutableStateFlow(false)
    val expanded: StateFlow<Boolean> = _expanded

    private val _availableRanges =
        MutableStateFlow<List<MonthRange>>(MonthRange.getAvailableRanges())
    val availableRanges: StateFlow<List<MonthRange>> = _availableRanges.asStateFlow()

    private val _selectedRange = MutableStateFlow<MonthRange>(
        MonthRange.CurrentMonth(
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        )
    )
    val selectedRange: StateFlow<MonthRange> = _selectedRange.asStateFlow()

    private val _periodExpanded = MutableStateFlow(false)
    val periodExpanded: StateFlow<Boolean> = _periodExpanded.asStateFlow()

    private val _chartData = MutableStateFlow<List<MonthlyChartData>>(emptyList())
    val chartData: StateFlow<List<MonthlyChartData>> = _chartData.asStateFlow()

    private val _shops = MutableStateFlow<List<ShopListDto>>(emptyList())
    val shops: StateFlow<List<ShopListDto>> = _shops

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statistics = MutableStateFlow<ChartStatistics?>(null)
    val statistics: StateFlow<ChartStatistics?> = _statistics.asStateFlow()

    private val _periodLabel = MutableStateFlow<String>("")
    val periodLabel: StateFlow<String> = _periodLabel.asStateFlow()

    // ── Purchase chart state ─────────────────────────────────────────────────

    private val _purchaseCategoryData =
        MutableStateFlow<List<PurchaseCategoryChartData>>(emptyList())
    val purchaseCategoryData: StateFlow<List<PurchaseCategoryChartData>> =
        _purchaseCategoryData.asStateFlow()

    private val _purchaseStatistics = MutableStateFlow<PurchaseChartStatistics?>(null)
    val purchaseStatistics: StateFlow<PurchaseChartStatistics?> =
        _purchaseStatistics.asStateFlow()

    private val _isPurchaseLoading = MutableStateFlow(false)
    val isPurchaseLoading: StateFlow<Boolean> = _isPurchaseLoading.asStateFlow()

    // ── Internal state ───────────────────────────────────────────────────────

    private var allSummaries: List<SummaryEntity> = emptyList()

    private val database = AppDatabase.getDatabase(application)
    private val summaryDao = database.summaryDao()
    private val firestore = FirebaseFirestore.getInstance(firebaseApp)

    // ── Public API ───────────────────────────────────────────────────────────

    fun loadScreenData(userAccessCode: AccessCode) = viewModelScope.launch {
        // Authenticate with SmartTracker Firebase before reading shops/{shopId}/months/
        val signedIn = suspendCoroutine<Boolean> { cont ->
            FirebaseAuth.getInstance(firebaseApp).signInAnonymously()
                .addOnSuccessListener { cont.resume(true) }
                .addOnFailureListener { e ->
                    Log.e("HomeScreenViewModel", "SmartTracker auth failed: ${e.message}")
                    cont.resume(false)
                }
        }
        if (!signedIn) return@launch

        _shops.value = getHomeShopUser(userAccessCode, database)
    }

    fun setSelectedShop(shop: ShopListDto?) {
        _selectedShop.value = shop
        shop?.let {
            if (!it.shopId.isNullOrEmpty()) {
                loadChartData(it.shopId)
            }
        }
    }

    fun setExpanded(value: Boolean) {
        _expanded.value = value
    }

    fun setSelectedRange(range: MonthRange) {
        _selectedRange.value = range
        _selectedShop.value?.shopId?.let { shopId ->
            loadChartData(shopId)
        }
    }

    fun setPeriodExpanded(value: Boolean) {
        _periodExpanded.value = value
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun loadChartData(shopId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            allSummaries = summaryDao.getAllSummariesForShop(shopId = shopId)
            // Recalculate targets so the 1500 minimum floor is enforced on cached data.
            // Only write back to Room if any target value actually changed.
            val recalculated = TargetSaleCalculator.calculateTargetSalesForShop(allSummaries)
            val oldTargetMap = allSummaries.associate { it.monthYear to it.targetSale }
            val targetsChanged = recalculated.any { it.targetSale != oldTargetMap[it.monthYear] }
            if (targetsChanged) {
                summaryDao.insertSummaries(recalculated)
                allSummaries = recalculated
            }
            val sortedData = allSummaries.sortedByDescending { it.monthTimestamp }

            val selectedRange = _selectedRange.value

            // skipCount = how many newest months to drop before taking chart data
            val (chartMonthCount, statsMonthCount, skipCount) = when (selectedRange) {
                is MonthRange.CurrentMonth        -> Triple(1, 1, 0)
                is MonthRange.PreviousMonth       -> Triple(1, 1, 1)
                is MonthRange.PreviousPreviousMonth -> Triple(1, 1, 2)
                is MonthRange.Last3Months         -> Triple(3, 3, 1)
                is MonthRange.Last6Months         -> Triple(6, 6, 1)
            }

            val dataToUse = sortedData.drop(skipCount)
            val chartMonths = dataToUse.take(chartMonthCount).reversed()

            _chartData.value = chartMonths.map { summary ->
                MonthlyChartData(
                    monthYear = summary.monthYear,
                    monthShortName = summary.monthYear.getShortMonthName(),
                    targetSale = summary.targetSale,
                    averageSale = summary.averageSale ?: 0.0,
                    isTargetMet = (summary.averageSale ?: 0.0) >= summary.targetSale
                )
            }

            val statsMonths = dataToUse.take(statsMonthCount)
            calculateStatistics(statsMonths)
            _periodLabel.value = generatePeriodLabel(statsMonths, selectedRange)

            // Purchase chart indices (newest-first).
            // Single-month ranges: one current + one previous for target baseline.
            // Multi-month ranges: aggregate actuals across the window; target = same-size prior window × 1.10.
            // Current Month  → [0] vs [1]
            // Previous Month → [1] vs [2]
            // Last 3 Months  → [1,2,3] vs [4,5,6]   (skips current incomplete month)
            // Last 6 Months  → [1,2,3,4,5,6] vs [7,8,9,10,11,12]
            val (purchaseCurrentIndices, purchasePreviousIndices) = when (selectedRange) {
                is MonthRange.CurrentMonth          -> Pair(listOf(0), listOf(1))
                is MonthRange.PreviousMonth         -> Pair(listOf(1), listOf(2))
                is MonthRange.PreviousPreviousMonth -> Pair(listOf(2), listOf(3))
                is MonthRange.Last3Months           -> Pair(listOf(1, 2, 3), listOf(2, 3, 4))
                is MonthRange.Last6Months           -> Pair(listOf(1, 2, 3, 4, 5, 6), listOf(2, 3, 4, 5, 6, 7))
            }
            loadPurchaseChartData(shopId, sortedData, purchaseCurrentIndices, purchasePreviousIndices)
        }
    }

    /**
     * Aggregates purchase breakdowns across [currentIndices] months (newest-first) for actuals,
     * and across [previousIndices] months for the target baseline.
     * For single-month periods (Current/Previous Month) each list has one entry.
     * For multi-month periods (Last 3/6 Months) each list has multiple entries — amounts are
     * summed per category across all months in the window.
     * Target = aggregated previous-window amount × 1.10, floored at MIN_PURCHASE_CATEGORY_TARGET.
     */
    private suspend fun loadPurchaseChartData(
        shopId: String,
        sortedSummaries: List<SummaryEntity>,
        currentIndices: List<Int>,
        previousIndices: List<Int>
    ) {
        if (sortedSummaries.isEmpty()) {
            _purchaseCategoryData.value = emptyList()
            _purchaseStatistics.value = null
            return
        }

        _isPurchaseLoading.value = true

        val currentMonthIds  = currentIndices.mapNotNull  { sortedSummaries.getOrNull(it)?.monthYear }
        val previousMonthIds = previousIndices.mapNotNull { sortedSummaries.getOrNull(it)?.monthYear }

        // Aggregate actuals: sum totalAmount per categoryId across all current-window months
        val currentAggregated = mutableMapOf<Int, PurchaseItem>()
        currentMonthIds.forEach { monthId ->
            fetchPurchaseBreakdown(shopId, monthId).forEach { item ->
                val existing = currentAggregated[item.categoryId]
                currentAggregated[item.categoryId] = if (existing != null)
                    existing.copy(totalAmount = existing.totalAmount + item.totalAmount)
                else
                    item
            }
        }

        // Aggregate previous window: sum totalAmount per categoryId for target baseline
        val previousAggregated = mutableMapOf<Int, Double>()
        previousMonthIds.forEach { monthId ->
            fetchPurchaseBreakdown(shopId, monthId).forEach { item ->
                previousAggregated[item.categoryId] =
                    (previousAggregated[item.categoryId] ?: 0.0) + item.totalAmount
            }
        }

        val chartItems = currentAggregated.values
            .sortedByDescending { it.totalAmount }
            .map { item ->
                val prevAmount = previousAggregated[item.categoryId]
                PurchaseCategoryChartData(
                    categoryId   = item.categoryId,
                    categoryName = item.categoryName,
                    actual       = item.totalAmount,
                    target       = if (prevAmount != null && prevAmount > 0)
                        maxOf(prevAmount * 1.10, MIN_PURCHASE_CATEGORY_TARGET)
                    else
                        MIN_PURCHASE_CATEGORY_TARGET
                )
            }

        val totalActual        = chartItems.sumOf { it.actual }
        val totalTarget        = maxOf(chartItems.sumOf { it.target }, MIN_TOTAL_PURCHASE_TARGET)
        val categoriesOnTarget = chartItems.count { !it.hasTarget || it.actual >= it.target }

        _purchaseCategoryData.value = chartItems
        _purchaseStatistics.value = PurchaseChartStatistics(
            totalActual        = totalActual,
            totalTarget        = totalTarget,
            monthLabel         = currentMonthIds.firstOrNull() ?: "",
            categoriesOnTarget = categoriesOnTarget,
            totalCategories    = chartItems.size
        )
        _isPurchaseLoading.value = false
    }

    /**
     * One-shot Firestore fetch for a single month's purchaseBreakdown array.
     * Returns an empty list on any error.
     */
    private suspend fun fetchPurchaseBreakdown(
        shopId: String,
        monthId: String
    ): List<PurchaseItem> = suspendCoroutine { cont ->
        firestore.collection("summary")
            .document(shopId)
            .collection("months")
            .document(monthId)
            .get()
            .addOnSuccessListener { document ->
                @Suppress("UNCHECKED_CAST")
                val raw = document.get("purchaseBreakdown") as? List<Map<String, Any>>
                    ?: emptyList()
                cont.resume(raw.map { map ->
                    PurchaseItem(
                        categoryId   = (map["categoryId"] as? Long)?.toInt() ?: 0,
                        categoryName = map["categoryName"] as? String ?: "Uncategorised",
                        totalAmount  = parseAmount(map["totalAmount"])
                    )
                })
            }
            .addOnFailureListener { e ->
                Log.e(
                    "HomeScreenViewModel",
                    "Error fetching purchaseBreakdown for $shopId / $monthId: ${e.message}"
                )
                cont.resume(emptyList())
            }
    }

    private fun parseAmount(value: Any?): Double = when (value) {
        is Double -> value
        is Long   -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: 0.0
        else      -> 0.0
    }

    private fun generatePeriodLabel(data: List<SummaryEntity>, range: MonthRange): String {
        if (data.isEmpty()) return ""
        val dateFormat  = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMMM yyyy",   Locale.getDefault())
        return when (range) {
            is MonthRange.CurrentMonth          -> dateFormat.format(Calendar.getInstance().time)
            is MonthRange.PreviousMonth         -> monthFormat.format(data[0].monthTimestamp)
            is MonthRange.PreviousPreviousMonth -> monthFormat.format(data[0].monthTimestamp)
            is MonthRange.Last3Months           -> "Last 3 Months"
            is MonthRange.Last6Months           -> "Last 6 Months"
        }
    }

    private fun calculateStatistics(data: List<SummaryEntity>) {
        if (data.isEmpty()) {
            _statistics.value = null
            return
        }
        val totalTarget   = data.sumOf { it.targetSale }
        val totalAverage  = data.sumOf { it.averageSale ?: 0.0 }
        val monthsTargetMet = data.count { (it.averageSale ?: 0.0) >= it.targetSale }

        _statistics.value = ChartStatistics(
            totalMonths              = data.size,
            totalTarget              = totalTarget,
            totalAverage             = totalAverage,
            monthsTargetMet          = monthsTargetMet,
            averageAchievementPercentage = if (totalTarget > 0) {
                (totalAverage / totalTarget) * 100.0
            } else 0.0,
            monthsTargetMetPercentage = if (data.isNotEmpty()) {
                (monthsTargetMet.toDouble() / data.size) * 100.0
            } else 0.0
        )
    }
}
