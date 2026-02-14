package com.marsa.smarttrackerhub.ui.screens.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.SummaryEntity
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.domain.ChartStatistics
import com.marsa.smarttrackerhub.domain.MonthRange
import com.marsa.smarttrackerhub.domain.getHomeShopUser
import com.marsa.smarttrackerhub.ui.screens.chart.MonthlyChartData
import com.marsa.smarttrackerhub.ui.screens.statement.ShopListDto
import com.marsa.smarttrackerhub.utils.getShortMonthName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeScreenViewModel(
    application: Application
) : ViewModel() {

    private val _selectedShop = MutableStateFlow<ShopListDto?>(null)
    val selectedShop: StateFlow<ShopListDto?> = _selectedShop

    private val _expanded = MutableStateFlow(false)
    val expanded: StateFlow<Boolean> = _expanded

    // Period selection
    private val _availableRanges = MutableStateFlow<List<MonthRange>>(MonthRange.getAvailableRanges())
    val availableRanges: StateFlow<List<MonthRange>> = _availableRanges.asStateFlow()

    private val _selectedRange = MutableStateFlow<MonthRange>(MonthRange.Last3Months)
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

    private var allSummaries: List<SummaryEntity> = emptyList()

    private val database = AppDatabase.getDatabase(application)
    private val summaryDao = database.summaryDao()

    fun loadScreenData(userAccessCode: AccessCode) {
        _shops.value = getHomeShopUser(userAccessCode)
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

    private fun loadChartData(shopId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            allSummaries = summaryDao.getAllSummariesForShop(shopId = shopId)
            val sortedData = allSummaries.sortedByDescending { it.monthTimestamp }

            val selectedRange = _selectedRange.value

            // Determine how many months to take and whether to skip current month
            val (chartMonthCount, statsMonthCount, skipCurrentMonth) = when (selectedRange) {
                is MonthRange.CurrentMonth -> Triple(1, 1, false)     // Show current, include in stats
                is MonthRange.PreviousMonth -> Triple(1, 1, true)     // Show previous, skip current
                is MonthRange.Last3Months -> Triple(3, 3, true)       // Show 3, skip current
                is MonthRange.Last6Months -> Triple(6, 6, true)       // Show 6, skip current
            }

            // Skip current month if needed
            val dataToUse = if (skipCurrentMonth) {
                sortedData.drop(1) // Skip the first (current) month
            } else {
                sortedData
            }

            // Get chart data
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

            // Get statistics data (same as chart data)
            val statsMonths = dataToUse.take(statsMonthCount)
            calculateStatistics(statsMonths)

            // Generate period label
            _periodLabel.value = generatePeriodLabel(statsMonths, selectedRange)
        }
    }

    private fun generatePeriodLabel(data: List<SummaryEntity>, range: MonthRange): String {
        if (data.isEmpty()) return ""

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        return when (range) {
            is MonthRange.CurrentMonth -> {
                // Show today's date
                dateFormat.format(Calendar.getInstance().time)
            }
            is MonthRange.PreviousMonth -> {
                // Show "Last Month"
                "Last Month"
            }
            is MonthRange.Last3Months -> {
                "Last 3 Months"
            }
            is MonthRange.Last6Months -> {
                "Last 6 Months"
            }
        }
    }

    private fun calculateStatistics(data: List<SummaryEntity>) {
        if (data.isEmpty()) {
            _statistics.value = null
            return
        }

        val totalTarget = data.sumOf { it.targetSale }
        val totalAverage = data.sumOf { it.averageSale ?: 0.0 }
        val monthsTargetMet = data.count { (it.averageSale ?: 0.0) >= it.targetSale }

        _statistics.value = ChartStatistics(
            totalMonths = data.size,
            totalTarget = totalTarget,
            totalAverage = totalAverage,
            monthsTargetMet = monthsTargetMet,
            averageAchievementPercentage = if (totalTarget > 0) {
                (totalAverage / totalTarget) * 100.0
            } else 0.0,
            monthsTargetMetPercentage = if (data.isNotEmpty()) {
                (monthsTargetMet.toDouble() / data.size) * 100.0
            } else 0.0
        )
    }
}