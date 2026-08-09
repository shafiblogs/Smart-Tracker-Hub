package com.marsa.smarttrackerhub.ui.screens.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.PurchaseEntity
import com.marsa.smarttrackerhub.data.entity.SummaryEntity
import com.marsa.smarttrackerhub.data.entity.toDomain
import com.marsa.smarttrackerhub.data.entity.toEntity
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.domain.AccountSummary
import com.marsa.smarttrackerhub.domain.ChartStatistics
import com.marsa.smarttrackerhub.domain.MonthRange
import com.marsa.smarttrackerhub.domain.MonthlySummary
import com.marsa.smarttrackerhub.domain.ShopRegion
import com.marsa.smarttrackerhub.domain.getHomeShopUser
import com.marsa.smarttrackerhub.domain.getSummaryShopList
import com.marsa.smarttrackerhub.ui.screens.chart.PurchaseCategoryChartData
import com.marsa.smarttrackerhub.ui.screens.chart.PurchaseChartStatistics
import com.marsa.smarttrackerhub.ui.screens.purchase.PurchaseItem
import com.marsa.smarttrackerhub.ui.screens.sale.TargetSaleCalculator
import com.marsa.smarttrackerhub.ui.screens.statement.ShopListDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val MIN_PURCHASE_CATEGORY_TARGET = 500.0
private const val MIN_TOTAL_PURCHASE_TARGET = 10000.0

/**
 * Home screen: a period selector (Current / Previous / Last 3 / Last 6 months) driving,
 * for the chosen period (UAE only):
 *   - one Account card (region-level aggregate, AccountTrackerApp), then
 *   - one Sales card (UnifiedStatisticsCard: sales + purchase stats) per shop.
 *
 * Same per-shop window math the old Home used, now rendered for every shop instead of one.
 */
class HomeScreenViewModel(
    application: Application,
    private val firebaseApp: FirebaseApp   // SmartTrackerApp (sales/purchase)
) : ViewModel() {

    data class ShopStats(
        val shop: ShopListDto,
        val sales: ChartStatistics?,
        val purchase: PurchaseChartStatistics?,
        val salesMargin: Double
    )

    data class RegionAccount(val region: ShopListDto, val summary: AccountSummary?)

    private val db = AppDatabase.getDatabase(application)
    private val summaryDao = db.summaryDao()
    private val purchaseDao = db.purchaseDao()
    private val accountDao = db.accountSummaryDao()
    private val salesFirestore = FirebaseFirestore.getInstance(firebaseApp)

    private val accountApp = runCatching { FirebaseApp.getInstance("AccountTrackerApp") }.getOrNull()
    private val accountFirestore = accountApp?.let { FirebaseFirestore.getInstance(it) }

    private val _availableRanges = MutableStateFlow(MonthRange.getAvailableRanges())
    val availableRanges: StateFlow<List<MonthRange>> = _availableRanges.asStateFlow()

    private val _selectedRange = MutableStateFlow<MonthRange>(
        MonthRange.CurrentMonth(
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        )
    )
    val selectedRange: StateFlow<MonthRange> = _selectedRange.asStateFlow()

    private val _accountCards = MutableStateFlow<List<RegionAccount>>(emptyList())
    val accountCards: StateFlow<List<RegionAccount>> = _accountCards.asStateFlow()

    private val _shopStats = MutableStateFlow<List<ShopStats>>(emptyList())
    val shopStats: StateFlow<List<ShopStats>> = _shopStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var shops: List<ShopListDto> = emptyList()
    private var regions: List<ShopListDto> = emptyList()
    // shopId → summaries sorted newest→oldest.
    private var salesByShop: Map<String, List<SummaryEntity>> = emptyMap()
    // Global month set (newest→oldest) for the account card's representative month.
    private var allMonthsSorted: List<String> = emptyList()

    fun loadScreenData(userAccessCode: AccessCode) = viewModelScope.launch {
        _isLoading.value = true
        signIn(firebaseApp)
        accountApp?.let { signIn(it) }

        withContext(Dispatchers.IO) {
            shops = getHomeShopUser(userAccessCode, db).filter { it.region == ShopRegion.UAE }
            regions = getSummaryShopList(userAccessCode).filter { it.region == ShopRegion.UAE }

            // Refresh ONLY the current calendar month from Firestore (best-effort) so the
            // in-progress month shows the latest sales/purchase/account, not stale Room data.
            val currentMonthId = SimpleDateFormat("MMMM - yyyy", Locale.getDefault())
                .format(Calendar.getInstance().time)
            shops.forEach { it.shopId?.let { id -> refreshSalesMonth(id, currentMonthId) } }
            regions.forEach { it.shopId?.let { id -> refreshAccountMonth(id, currentMonthId) } }

            val byShop = mutableMapOf<String, List<SummaryEntity>>()
            val monthTimestamps = mutableMapOf<String, Long>()
            shops.forEach { shop ->
                val id = shop.shopId ?: return@forEach
                var list = summaryDao.getAllSummariesForShop(id)
                val recalced = TargetSaleCalculator.calculateTargetSalesForShop(list)
                val oldTargets = list.associate { it.monthYear to it.targetSale }
                if (recalced.any { it.targetSale != oldTargets[it.monthYear] }) {
                    summaryDao.insertSummaries(recalced)
                    list = recalced
                }
                byShop[id] = list.sortedByDescending { it.monthTimestamp }
                list.forEach { monthTimestamps[it.monthYear] = it.monthTimestamp }
            }
            salesByShop = byShop
            allMonthsSorted = monthTimestamps.entries.sortedByDescending { it.value }.map { it.key }
        }
        recompute()
    }

    fun setSelectedRange(range: MonthRange) {
        _selectedRange.value = range
        recompute()
    }

    private fun recompute() = viewModelScope.launch {
        _isLoading.value = true
        val range = _selectedRange.value

        _shopStats.value = withContext(Dispatchers.IO) {
            shops.map { shop -> buildShopStats(shop, range) }
        }

        // Account card uses the representative (newest-in-window) month.
        val skip = windowFor(range).third
        val accountMonth = allMonthsSorted.getOrNull(skip) ?: allMonthsSorted.firstOrNull()
        _accountCards.value = withContext(Dispatchers.IO) {
            regions.map { region ->
                RegionAccount(region, region.shopId?.takeIf { accountMonth != null }
                    ?.let { fetchAccount(it, accountMonth!!) })
            }
        }
        _isLoading.value = false
    }

    /** (chartMonthCount, statsMonthCount, skipCount) — same windows as the old Home. */
    private fun windowFor(range: MonthRange): Triple<Int, Int, Int> = when (range) {
        is MonthRange.CurrentMonth          -> Triple(1, 1, 0)
        is MonthRange.PreviousMonth         -> Triple(1, 1, 1)
        is MonthRange.PreviousPreviousMonth -> Triple(1, 1, 2)
        is MonthRange.Last3Months           -> Triple(3, 3, 1)
        is MonthRange.Last6Months           -> Triple(6, 6, 1)
    }

    /** Purchase current-window vs previous-window indices into the shop's newest→oldest list. */
    private fun purchaseIndicesFor(range: MonthRange): Pair<List<Int>, List<Int>> = when (range) {
        is MonthRange.CurrentMonth          -> listOf(0) to listOf(1)
        is MonthRange.PreviousMonth         -> listOf(1) to listOf(2)
        is MonthRange.PreviousPreviousMonth -> listOf(2) to listOf(3)
        is MonthRange.Last3Months           -> listOf(1, 2, 3) to listOf(4, 5, 6)
        is MonthRange.Last6Months           -> (1..6).toList() to (7..12).toList()
    }

    private suspend fun buildShopStats(shop: ShopListDto, range: MonthRange): ShopStats {
        val id = shop.shopId
        val list = id?.let { salesByShop[it] } ?: emptyList()
        if (id == null || list.isEmpty()) return ShopStats(shop, null, null, 0.0)

        val (_, statsCount, skip) = windowFor(range)
        val statsMonths = list.drop(skip).take(statsCount)
        if (statsMonths.isEmpty()) return ShopStats(shop, null, null, 0.0)

        val totalTarget = statsMonths.sumOf { it.targetSale }
        val totalAverage = statsMonths.sumOf { it.averageSale ?: 0.0 }
        val monthsTargetMet = statsMonths.count { (it.averageSale ?: 0.0) >= it.targetSale }
        val sales = ChartStatistics(
            totalMonths = statsMonths.size,
            totalTarget = totalTarget,
            totalAverage = totalAverage,
            monthsTargetMet = monthsTargetMet,
            averageAchievementPercentage = if (totalTarget > 0) totalAverage / totalTarget * 100.0 else 0.0,
            monthsTargetMetPercentage = if (statsMonths.isNotEmpty())
                monthsTargetMet.toDouble() / statsMonths.size * 100.0 else 0.0
        )

        // Purchase: aggregate current vs previous window (target = prev × 1.10, floored).
        val (curIdx, prevIdx) = purchaseIndicesFor(range)
        val currentAgg = aggregateBreakdown(id, list, curIdx)
        val prevAgg = aggregateBreakdownAmounts(id, list, prevIdx)
        val chart = currentAgg.values.sortedByDescending { it.totalAmount }.map { item ->
            val prev = prevAgg[item.categoryId]
            PurchaseCategoryChartData(
                categoryId = item.categoryId,
                categoryName = item.categoryName,
                actual = item.totalAmount,
                target = if (prev != null && prev > 0)
                    maxOf(prev * 1.10, MIN_PURCHASE_CATEGORY_TARGET)
                else MIN_PURCHASE_CATEGORY_TARGET
            )
        }
        val purchase = PurchaseChartStatistics(
            totalActual = chart.sumOf { it.actual },
            totalTarget = maxOf(chart.sumOf { it.target }, MIN_TOTAL_PURCHASE_TARGET),
            monthLabel = range.displayName,
            categoriesOnTarget = chart.count { !it.hasTarget || it.actual >= it.target },
            totalCategories = chart.size
        )

        val totalSales = statsMonths.sumOf { it.totalSales }
        val totalPurchases = statsMonths.sumOf { it.totalPurchases }
        val margin = if (totalSales > 0) (totalSales - totalPurchases) / totalSales * 100.0 else 0.0

        return ShopStats(shop, sales, purchase, margin)
    }

    private suspend fun aggregateBreakdown(
        shopId: String, list: List<SummaryEntity>, indices: List<Int>
    ): Map<Int, PurchaseItem> {
        val agg = mutableMapOf<Int, PurchaseItem>()
        indices.mapNotNull { list.getOrNull(it)?.monthYear }.forEach { month ->
            fetchPurchaseBreakdown(shopId, month).forEach { item ->
                val existing = agg[item.categoryId]
                agg[item.categoryId] = existing?.copy(totalAmount = existing.totalAmount + item.totalAmount)
                    ?: item
            }
        }
        return agg
    }

    private suspend fun aggregateBreakdownAmounts(
        shopId: String, list: List<SummaryEntity>, indices: List<Int>
    ): Map<Int, Double> {
        val agg = mutableMapOf<Int, Double>()
        indices.mapNotNull { list.getOrNull(it)?.monthYear }.forEach { month ->
            fetchPurchaseBreakdown(shopId, month).forEach { item ->
                agg[item.categoryId] = (agg[item.categoryId] ?: 0.0) + item.totalAmount
            }
        }
        return agg
    }

    /** Force-fetch one month's sales summary + purchase breakdown from Firestore into Room. */
    private suspend fun refreshSalesMonth(shopId: String, monthId: String) {
        @Suppress("UNCHECKED_CAST")
        val doc = suspendCoroutine<com.google.firebase.firestore.DocumentSnapshot?> { cont ->
            salesFirestore.collection("summary").document(shopId)
                .collection("months").document(monthId).get()
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        } ?: return
        if (!doc.exists()) return

        doc.toObject(MonthlySummary::class.java)?.let { s ->
            val stamped = if (s.lastUpdated == 0L) s.copy(lastUpdated = System.currentTimeMillis()) else s
            runCatching { summaryDao.insertSummary(stamped.toEntity(shopId, monthId)) }
        }

        @Suppress("UNCHECKED_CAST")
        val breakdown = doc.get("purchaseBreakdown") as? List<Map<String, Any>> ?: emptyList()
        if (breakdown.isNotEmpty()) {
            val entities = breakdown.map { m ->
                PurchaseEntity(
                    shopId = shopId,
                    monthId = monthId,
                    categoryId = (m["categoryId"] as? Long)?.toInt() ?: 0,
                    categoryName = m["categoryName"] as? String ?: "Uncategorised",
                    totalAmount = parseAmount(m["totalAmount"]),
                    lastUpdated = System.currentTimeMillis()
                )
            }
            runCatching {
                purchaseDao.deletePurchasesForMonth(shopId, monthId)
                purchaseDao.insertPurchases(entities)
            }
        }
    }

    /** Force-fetch one month's account summary from Firestore into Room. */
    private suspend fun refreshAccountMonth(shopId: String, monthId: String) {
        val fs = accountFirestore ?: return
        val summary = suspendCoroutine<AccountSummary?> { cont ->
            fs.collection("summary").document(shopId).collection("months").document(monthId).get()
                .addOnSuccessListener { doc ->
                    cont.resume(
                        doc.toObject(AccountSummary::class.java)?.let {
                            if (it.lastUpdated == 0L) it.copy(lastUpdated = System.currentTimeMillis()) else it
                        }
                    )
                }
                .addOnFailureListener { cont.resume(null) }
        }
        if (summary != null) runCatching { accountDao.insertAccountSummary(summary.toEntity(shopId, monthId)) }
    }

    private suspend fun fetchPurchaseBreakdown(shopId: String, monthId: String): List<PurchaseItem> {
        runCatching {
            val cached = purchaseDao.getPurchasesForMonth(shopId, monthId)
            if (cached.isNotEmpty()) {
                return cached.map { PurchaseItem(it.categoryId, it.categoryName, it.totalAmount) }
            }
        }
        @Suppress("UNCHECKED_CAST")
        return suspendCoroutine { cont ->
            salesFirestore.collection("summary").document(shopId)
                .collection("months").document(monthId).get()
                .addOnSuccessListener { doc ->
                    val raw = doc.get("purchaseBreakdown") as? List<Map<String, Any>> ?: emptyList()
                    cont.resume(raw.map { m ->
                        PurchaseItem(
                            categoryId = (m["categoryId"] as? Long)?.toInt() ?: 0,
                            categoryName = m["categoryName"] as? String ?: "Uncategorised",
                            totalAmount = parseAmount(m["totalAmount"])
                        )
                    })
                }
                .addOnFailureListener { cont.resume(emptyList()) }
        }
    }

    private suspend fun fetchAccount(shopId: String, month: String): AccountSummary? {
        accountDao.getAccountSummary(shopId, month)?.let { return it.toDomain() }
        val fs = accountFirestore ?: return null
        val summary = suspendCoroutine<AccountSummary?> { cont ->
            fs.collection("summary").document(shopId).collection("months").document(month).get()
                .addOnSuccessListener { doc ->
                    cont.resume(
                        doc.toObject(AccountSummary::class.java)?.let {
                            if (it.lastUpdated == 0L) it.copy(lastUpdated = System.currentTimeMillis()) else it
                        }
                    )
                }
                .addOnFailureListener { cont.resume(null) }
        }
        if (summary != null) runCatching { accountDao.insertAccountSummary(summary.toEntity(shopId, month)) }
        return summary
    }

    private fun parseAmount(v: Any?): Double = when (v) {
        is Double -> v
        is Long -> v.toDouble()
        is String -> v.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    private suspend fun signIn(app: FirebaseApp): Boolean = suspendCoroutine { cont ->
        FirebaseAuth.getInstance(app).signInAnonymously()
            .addOnSuccessListener { cont.resume(true) }
            .addOnFailureListener {
                Log.e("HomeScreenViewModel", "auth failed for ${app.name}: ${it.message}")
                cont.resume(false)
            }
    }
}
