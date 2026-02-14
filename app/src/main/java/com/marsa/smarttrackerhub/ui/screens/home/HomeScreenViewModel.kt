package com.marsa.smarttrackerhub.ui.screens.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.SummaryEntity
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.domain.ChartStatistics
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

    private fun loadChartData(shopId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            allSummaries = summaryDao.getAllSummariesForShop(shopId = shopId)

            val sortedData = allSummaries.sortedByDescending { it.monthTimestamp }

            // Take last 6 months for chart display
            val chartMonths = sortedData.take(6).reversed()

            _chartData.value = chartMonths.map { summary ->
                MonthlyChartData(
                    monthYear = summary.monthYear,
                    monthShortName = summary.monthYear.getShortMonthName(),
                    targetSale = summary.targetSale,
                    averageSale = summary.averageSale ?: 0.0,
                    isTargetMet = (summary.averageSale ?: 0.0) >= summary.targetSale
                )
            }

            // Use only current month for statistics
            val statsMonths = sortedData.take(1)
            calculateStatistics(statsMonths)

            // Generate period label
            _periodLabel.value = generatePeriodLabel(statsMonths)
        }
    }

    /**
     * Generates dynamic period label based on data
     */
    private fun generatePeriodLabel(data: List<SummaryEntity>): String {
        if (data.isEmpty()) return ""

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val currentDate = Calendar.getInstance()

        // Get the month from the data
        val dataMonth = Calendar.getInstance().apply {
            timeInMillis = data[0].monthTimestamp
        }

        // Check if it's current month
        val isCurrentMonth = currentDate.get(Calendar.YEAR) == dataMonth.get(Calendar.YEAR) &&
                currentDate.get(Calendar.MONTH) == dataMonth.get(Calendar.MONTH)

        return if (isCurrentMonth) {
            // Current month: show today's date
            dateFormat.format(currentDate.time)
        } else {
            // Previous months: calculate difference
            val monthsDiff = (currentDate.get(Calendar.YEAR) - dataMonth.get(Calendar.YEAR)) * 12 +
                    (currentDate.get(Calendar.MONTH) - dataMonth.get(Calendar.MONTH))

            when (monthsDiff) {
                1 -> "Last Month"
                2 -> "Last 2 Months"
                3 -> "Last 3 Months"
                else -> "Last $monthsDiff Months"
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