package com.marsa.smarttrackerhub.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.entity.SummaryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing sales chart data
 */
class SalesChartViewModel(
    // Inject your repository here
    // private val repository: SalesRepository
) : ViewModel() {

    private val _selectedShopId = MutableStateFlow<String?>(null)
    val selectedShopId: StateFlow<String?> = _selectedShopId.asStateFlow()

    private val _chartData = MutableStateFlow<List<MonthlyChartData>>(emptyList())
    val chartData: StateFlow<List<MonthlyChartData>> = _chartData.asStateFlow()

    private val _availableShops = MutableStateFlow<List<ShopInfo>>(emptyList())
    val availableShops: StateFlow<List<ShopInfo>> = _availableShops.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statistics = MutableStateFlow<ChartStatistics?>(null)
    val statistics: StateFlow<ChartStatistics?> = _statistics.asStateFlow()

    // In-memory cache
    private var allSummaries: List<SummaryEntity> = emptyList()

    /**
     * Loads all summary data
     */
    fun loadData(summaries: List<SummaryEntity>) {
        viewModelScope.launch {
            _isLoading.value = true

            allSummaries = summaries

            // Extract unique shops
            val shops = summaries
                .map { it.shopId }
                .distinct()
                .sorted()
                .map { shopId ->
                    ShopInfo(
                        shopId = shopId,
                        displayName = formatShopName(shopId)
                    )
                }

            _availableShops.value = shops

            // Select first shop by default
            if (_selectedShopId.value == null && shops.isNotEmpty()) {
                selectShop(shops.first().shopId)
            }

            _isLoading.value = false
        }
    }

    /**
     * Selects a shop and loads its data
     */
    fun selectShop(shopId: String) {
        _selectedShopId.value = shopId
        loadChartData(shopId)
    }

    /**
     * Loads chart data for selected shop (last 6 months)
     */
    private fun loadChartData(shopId: String) {
        val shopData = allSummaries
            .filter { it.shopId == shopId }
            .sortedByDescending { it.monthTimestamp } // Sort by timestamp descending (newest first)
            .take(6) // Take last 6 months
            .reversed() // Reverse to show oldest to newest in chart

        _chartData.value = shopData.map { summary ->
            MonthlyChartData(
                monthYear = summary.monthYear,
                monthShortName = getShortMonthName(summary.monthYear),
                targetSale = summary.targetSale,
                averageSale = summary.averageSale ?: 0.0,
                isTargetMet = (summary.averageSale ?: 0.0) >= summary.targetSale
            )
        }

        // Calculate statistics
        calculateStatistics(shopData)
    }

    /**
     * Calculates summary statistics
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
     * Formats shop ID to display name
     */
    private fun formatShopName(shopId: String): String {
        // Convert MARSA_102 to "MARSA 102"
        return shopId.replace("_", " ")
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
