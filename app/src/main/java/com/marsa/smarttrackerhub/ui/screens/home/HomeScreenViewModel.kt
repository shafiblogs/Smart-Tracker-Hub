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
import com.marsa.smarttrackerhub.domain.MonthOption
import com.marsa.smarttrackerhub.domain.MonthSelection
import com.marsa.smarttrackerhub.domain.MonthlySummary
import com.marsa.smarttrackerhub.domain.ShopRegion
import com.marsa.smarttrackerhub.domain.getHomeShopUser
import com.marsa.smarttrackerhub.domain.getSummaryShopList
import com.marsa.smarttrackerhub.helper.getLastHomeAutoSyncTime
import com.marsa.smarttrackerhub.helper.saveLastHomeAutoSyncTime
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
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val MIN_PURCHASE_CATEGORY_TARGET = 500.0
private const val MIN_TOTAL_PURCHASE_TARGET = 10000.0

/** Period-picker entries for Home: current month, then the two trailing ranges.
 *  The "Pick a month…" entry is always available as an additional option. */
private fun defaultRanges(): List<MonthSelection> {
    val current = YearMonth.now()
    return listOf(
        MonthSelection.Month(MonthOption(current)),
        MonthSelection.TrailingRange(3),
        MonthSelection.TrailingRange(6)
    )
}

/**
 * Home screen: a period selector (Current Month / Pick a Month / Last 3 / Last 6 months) driving,
 * for the chosen period (UAE only):
 *   - one Account card (region-level aggregate, AccountTrackerApp), then
 *   - one Sales card (UnifiedStatisticsCard: sales + purchase stats) per shop.
 *
 * Same per-shop window math the old Home used, now rendered for every shop instead of one.
 *
 * The period selector's free "pick a month" entry is deliberately NOT wired in yet: [windowFor]
 * and [purchaseIndicesFor] compute a skip count that indexes into [allMonthsSorted] — the months
 * a shop actually has cached — rather than a calendar distance. Those agree only when there's no
 * gap between "now" and a shop's most recent cached month. Resolving an arbitrary picked month
 * correctly means switching to a lookup-by-key (fetch-then-locate), which is a separate change.
 */
class HomeScreenViewModel(
    private val application: Application,
    private val firebaseApp: FirebaseApp   // SmartTrackerApp (sales/purchase)
) : ViewModel() {

    companion object {
        // Home's auto-refresh (fired every time the screen opens) is throttled to this
        // interval — repeated app opens/navigations within the window reuse the Room cache
        // instead of re-hitting Firestore every time. A user-driven period change
        // (setSelectedRange) always refreshes regardless of this timer.
        private const val AUTO_SYNC_INTERVAL_MS = 4 * 60 * 60 * 1000L // 4 hours
    }

    data class ShopStats(
        val shop: ShopListDto,
        val sales: ChartStatistics?,
        val purchase: PurchaseChartStatistics?,
        val salesMargin: Double,
        val lastUpdated: Long = 0L
    )

    data class RegionAccount(val region: ShopListDto, val summary: AccountSummary?)

    private val db = AppDatabase.getDatabase(application)
    private val summaryDao = db.summaryDao()
    private val purchaseDao = db.purchaseDao()
    private val accountDao = db.accountSummaryDao()
    private val salesFirestore = FirebaseFirestore.getInstance(firebaseApp)

    private val accountApp = runCatching { FirebaseApp.getInstance("AccountTrackerApp") }.getOrNull()
    private val accountFirestore = accountApp?.let { FirebaseFirestore.getInstance(it) }

    private val _availableRanges = MutableStateFlow(defaultRanges())
    val availableRanges: StateFlow<List<MonthSelection>> = _availableRanges.asStateFlow()

    private val _selectedRange = MutableStateFlow<MonthSelection>(MonthSelection.currentMonth())
    val selectedRange: StateFlow<MonthSelection> = _selectedRange.asStateFlow()

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
        _isLoading.value = true   // Show loading while reading from Room
        signIn(firebaseApp)
        accountApp?.let { signIn(it) }

        // 1) Load from Room cache and display data (may show loading briefly if it takes time).
        withContext(Dispatchers.IO) {
            shops = getHomeShopUser(userAccessCode, db).filter { it.region == ShopRegion.UAE }
            regions = getSummaryShopList(userAccessCode).filter { it.region == ShopRegion.UAE }
            buildIndexFromRoom()
        }
        computeStats()
        _isLoading.value = false   // Data ready — hide loading after Room read completes

        // 2) Refresh from Firestore only if 4+ hours have passed since last auto-sync.
        //    Show loading again only if fetching from Firestore.
        val now = System.currentTimeMillis()
        val lastSyncTime = getLastHomeAutoSyncTime(application)
        val dueForAutoSync = now - lastSyncTime >= AUTO_SYNC_INTERVAL_MS

        if (dueForAutoSync) {
            _isLoading.value = true   // Show loading for Firestore fetch
            withContext(Dispatchers.IO) {
                refreshSelectedPeriod(_selectedRange.value)
                buildIndexFromRoom()
            }
            computeStats()
            saveLastHomeAutoSyncTime(application, now)
            _isLoading.value = false
        }
    }

    fun setSelectedRange(range: MonthSelection) {
        _selectedRange.value = range
        viewModelScope.launch {
            _isLoading.value = true   // Show loading when user changes month
            // Refresh all months in the newly selected period from Firestore
            withContext(Dispatchers.IO) {
                refreshSelectedPeriod(range)
                buildIndexFromRoom()
            }
            computeStats()
            _isLoading.value = false  // Hide loading after stats computed
        }
    }

    /** Build the shop→summaries index + month list from Room only (no network). */
    private suspend fun buildIndexFromRoom() {
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

    /** Firestore refresh of all months in the selected period (sales + account). */
    private suspend fun refreshSelectedPeriod(range: MonthSelection) {
        val (_, statsCount, skip) = windowFor(range)
        // Get the months to refresh: skip the first 'skip' months, then take 'statsCount' months
        val monthsToRefresh = allMonthsSorted.drop(skip).take(statsCount)

        monthsToRefresh.forEach { monthId ->
            shops.forEach { it.shopId?.let { id -> refreshSalesMonth(id, monthId) } }
            regions.forEach { it.shopId?.let { id -> refreshAccountMonth(id, monthId) } }
        }
    }

    /** Compute shop + account cards for the selected period. Does NOT toggle isLoading. */
    private suspend fun computeStats() {
        val range = _selectedRange.value
        // Sales first (fast, Room-backed) so the UI paints immediately…
        _shopStats.value = withContext(Dispatchers.IO) {
            shops.map { shop -> buildShopStats(shop, range) }
        }
        // …then the account card(s) for the representative (newest-in-window) month.
        val accountMonth = allMonthsSorted.getOrNull(windowFor(range).third)
            ?: allMonthsSorted.firstOrNull()
        _accountCards.value = withContext(Dispatchers.IO) {
            regions.map { region ->
                RegionAccount(region, region.shopId?.takeIf { accountMonth != null }
                    ?.let { fetchAccount(it, accountMonth!!) })
            }
        }
    }

    /**
     * How many calendar months back from "now" a [MonthSelection.Month] sits — 0 for the
     * current month, 1 for last month, etc. Only exact for the built-in relative presets
     * ([defaultRanges]); see the class doc for why an arbitrary picked month isn't wired in yet.
     */
    private fun relativeSkipFor(month: MonthSelection.Month): Int =
        ChronoUnit.MONTHS.between(month.option.yearMonth, YearMonth.now()).toInt().coerceAtLeast(0)

    /** (chartMonthCount, statsMonthCount, skipCount) — same windows as the old Home. */
    private fun windowFor(range: MonthSelection): Triple<Int, Int, Int> = when (range) {
        is MonthSelection.Month         -> Triple(1, 1, relativeSkipFor(range))
        is MonthSelection.TrailingRange -> Triple(range.monthCount, range.monthCount, 1)
    }

    /** Purchase current-window vs previous-window indices into the shop's newest→oldest list. */
    private fun purchaseIndicesFor(range: MonthSelection): Pair<List<Int>, List<Int>> = when (range) {
        is MonthSelection.Month -> {
            val skip = relativeSkipFor(range)
            listOf(skip) to listOf(skip + 1)
        }
        is MonthSelection.TrailingRange -> {
            val n = range.monthCount
            (1..n).toList() to (n + 1..2 * n).toList()
        }
    }

    private suspend fun buildShopStats(shop: ShopListDto, range: MonthSelection): ShopStats {
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
        val lastUpdated = statsMonths.maxOf { it.lastUpdated }

        return ShopStats(shop, sales, purchase, margin, lastUpdated)
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
