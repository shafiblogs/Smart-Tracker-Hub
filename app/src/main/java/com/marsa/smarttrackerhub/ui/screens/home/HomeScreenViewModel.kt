package com.marsa.smarttrackerhub.ui.screens.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
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

class HomeScreenViewModel(
    application: Application,
    firebaseApp: FirebaseApp
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
            val sortedData = allSummaries.sortedByDescending { it.monthTimestamp }

            val selectedRange = _selectedRange.value

            val (chartMonthCount, statsMonthCount, skipCurrentMonth) = when (selectedRange) {
                is MonthRange.CurrentMonth  -> Triple(1, 1, false)
                is MonthRange.PreviousMonth -> Triple(1, 1, true)
                is MonthRange.Last3Months   -> Triple(3, 3, true)
                is MonthRange.Last6Months   -> Triple(6, 6, true)
            }

            val dataToUse = if (skipCurrentMonth) sortedData.drop(1) else sortedData
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

            // Purchase chart always uses the most recent month regardless of period selector
            loadPurchaseChartData(shopId, sortedData)
        }
    }

    /**
     * Fetches purchase breakdowns for the two most recent months, then builds
     * [PurchaseCategoryChartData] where target = previousMonth.totalAmount × 1.10.
     */
    private suspend fun loadPurchaseChartData(
        shopId: String,
        sortedSummaries: List<SummaryEntity>
    ) {
        if (sortedSummaries.isEmpty()) {
            _purchaseCategoryData.value = emptyList()
            _purchaseStatistics.value = null
            return
        }

        _isPurchaseLoading.value = true

        val currentMonthId  = sortedSummaries[0].monthYear
        val previousMonthId = sortedSummaries.getOrNull(1)?.monthYear

        val currentBreakdown  = fetchPurchaseBreakdown(shopId, currentMonthId)
        val previousBreakdown = if (previousMonthId != null)
            fetchPurchaseBreakdown(shopId, previousMonthId) else emptyList()

        // Map: categoryId → previous month's totalAmount
        val prevAmountMap = previousBreakdown.associateBy({ it.categoryId }, { it.totalAmount })

        val chartItems = currentBreakdown
            .filter { it.categoryName.isNotBlank() }
            .sortedByDescending { it.totalAmount }
            .map { item ->
                val prevAmount = prevAmountMap[item.categoryId]
                PurchaseCategoryChartData(
                    categoryId   = item.categoryId,
                    categoryName = item.categoryName,
                    actual       = item.totalAmount,
                    target       = if (prevAmount != null) prevAmount * 1.10 else 0.0
                )
            }

        val totalActual           = chartItems.sumOf { it.actual }
        val totalTarget           = chartItems.sumOf { it.target }
        // "on target" = actual reached or exceeded the target (more purchase = more sale = good)
        val categoriesUnderTarget = chartItems.count { !it.hasTarget || it.actual >= it.target }

        _purchaseCategoryData.value = chartItems
        _purchaseStatistics.value = PurchaseChartStatistics(
            totalActual           = totalActual,
            totalTarget           = totalTarget,
            monthLabel            = currentMonthId,
            categoriesUnderTarget = categoriesUnderTarget,
            totalCategories       = chartItems.size
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
                        categoryName = map["categoryName"] as? String ?: "",
                        totalAmount  = (map["totalAmount"] as? Double)
                            ?: (map["totalAmount"] as? Long)?.toDouble() ?: 0.0
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

    private fun generatePeriodLabel(data: List<SummaryEntity>, range: MonthRange): String {
        if (data.isEmpty()) return ""
        val dateFormat  = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMMM yyyy",   Locale.getDefault())
        return when (range) {
            is MonthRange.CurrentMonth  -> dateFormat.format(Calendar.getInstance().time)
            is MonthRange.PreviousMonth -> monthFormat.format(data[0].monthTimestamp)
            is MonthRange.Last3Months   -> "Last 3 Months"
            is MonthRange.Last6Months   -> "Last 6 Months"
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
