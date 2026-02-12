package com.marsa.smarttrackerhub.ui.screens.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.SummaryEntity
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.domain.getHomeShopUser
import com.marsa.smarttrackerhub.ui.screens.statement.ShopListDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing sales chart data
 */
class SalesChartViewModel(
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

    // In-memory cache
    private var allSummaries: List<SummaryEntity> = emptyList()

    private val database = AppDatabase.getDatabase(application)

    private val summaryDao = database.summaryDao()

    /**
     * Loads all summary data
     */
    fun loadScreenData(userAccessCode: AccessCode) {
        _shops.value = getHomeShopUser(userAccessCode)
    }

    /**
     * Selects a shop and loads its data
     */
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

    /**
     * Loads chart data for selected shop
     * - Displays last 4 months in chart
     * - Calculates statistics for last 3 months only (excluding current month)
     */
    private fun loadChartData(shopId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            allSummaries = summaryDao.getAllSummariesForShop(shopId = shopId)

            // Sort by timestamp descending (newest first)
            val sortedData = allSummaries.sortedByDescending { it.monthTimestamp }
            
            // Take last 4 months for chart display
            val chartMonths = sortedData.take(6).reversed() // Reverse to show oldest to newest

            _chartData.value = chartMonths.map { summary ->
                MonthlyChartData(
                    monthYear = summary.monthYear,
                    monthShortName = getShortMonthName(summary.monthYear),
                    targetSale = summary.targetSale,
                    averageSale = summary.averageSale ?: 0.0,
                    isTargetMet = (summary.averageSale ?: 0.0) >= summary.targetSale
                )
            }

            // Calculate statistics for last 3 months only (skip current/most recent month)
            // Skip index 0 (most recent) and take next 3
            /*val statsMonths = if (sortedData.size > 1) {
                sortedData.drop(1).take(3) // Skip current month, take next 3
            } else {
                emptyList() // Not enough data
            }
            
            calculateStatistics(statsMonths)*/
            calculateStatistics(sortedData.take(1))
        }
    }

    /**
     * Calculates summary statistics
     * Now uses only last 3 months (excluding current month)
     */
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

    /**
     * Gets short month name from monthYear string
     */
    private fun getShortMonthName(monthYear: String): String {
        return try {
            val parts = monthYear.split(" - ")
            if (parts.isNotEmpty()) {
                val month = parts[0].trim()
                month.take(3) // First 3 letters
            } else {
                monthYear
            }
        } catch (e: Exception) {
            monthYear
        }
    }
}

/**
 * Shop information
 */
data class ShopInfo(
    val shopId: String,
    val displayName: String
)

/**
 * Chart statistics
 */
data class ChartStatistics(
    val totalMonths: Int,
    val totalTarget: Double,
    val totalAverage: Double,
    val monthsTargetMet: Int,
    val averageAchievementPercentage: Double,
    val monthsTargetMetPercentage: Double
)
