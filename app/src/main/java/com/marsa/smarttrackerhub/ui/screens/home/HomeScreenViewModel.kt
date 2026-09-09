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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
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
 * Every window is resolved to an explicit, calendar-derived list of month keys
 * ([monthKeysFor]) which is then looked up by key in Room. The screen therefore shows exactly
 * the months the user asked for — "Last 3 Months" is always the three completed months before
 * the current one, for every shop — and a month with no cached record is reported as missing
 * (see [ShopStats.monthsCovered]) rather than being silently replaced by an older one.
 *
 * This replaces the previous positional math (`list.drop(skip).take(n)`), which indexed into
 * whatever months a shop happened to have cached: a shop missing one month slid its whole window
 * back, so each card could cover a different span, and an arbitrary picked month could not be
 * resolved at all.
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

    /**
     * [monthsCovered] months of the [monthsExpected] the selected period asks for actually had a
     * record in Room; the figures below aggregate exactly those. They differ when a shop has no
     * summary for part of the window.
     */
    data class ShopStats(
        val shop: ShopListDto,
        val sales: ChartStatistics?,
        val purchase: PurchaseChartStatistics?,
        val salesMargin: Double,
        val lastUpdated: Long = 0L,
        val monthsCovered: Int = 0,
        val monthsExpected: Int = 0
    )

    data class RegionAccount(
        val region: ShopListDto,
        val summary: AccountSummary?,
        val monthsCovered: Int = 0,
        val monthsExpected: Int = 0
    )


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
    // shopId → (month key → summary). Keyed by the "MonthName - yyyy" document convention so a
    // window can be resolved by calendar key instead of by position in a cached list.
    private var salesByShop: Map<String, Map<String, SummaryEntity>> = emptyMap()

    fun loadScreenData(userAccessCode: AccessCode) = viewModelScope.launch {
        _isLoading.value = true   // Show loading while reading from Room
        signIn(firebaseApp)
        accountApp?.let { signIn(it) }

        // Rebuilt on every load: the presets are relative to "now", so one computed at ViewModel
        // construction would still name the old month after crossing a month boundary.
        _availableRanges.value = defaultRanges()

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

        // The throttle must not strand a window that is incomplete locally: without this, a month
        // missing from Room stays missing on every reopen until the 4-hour timer happens to lapse.
        val incomplete = withContext(Dispatchers.IO) { hasMissingMonths(_selectedRange.value) }

        if (dueForAutoSync || incomplete) {
            _isLoading.value = true   // Show loading for Firestore fetch
            withContext(Dispatchers.IO) {
                refreshSelectedPeriod(_selectedRange.value)
                buildIndexFromRoom()
            }
            computeStats()
            if (dueForAutoSync) saveLastHomeAutoSyncTime(application, now)
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

    /** Build the shop→(monthKey→summary) index from Room only (no network). */
    private suspend fun buildIndexFromRoom() {
        val byShop = mutableMapOf<String, Map<String, SummaryEntity>>()
        shops.forEach { shop ->
            val id = shop.shopId ?: return@forEach
            var list = summaryDao.getAllSummariesForShop(id)
            val recalced = TargetSaleCalculator.calculateTargetSalesForShop(list)
            val oldTargets = list.associate { it.monthYear to it.targetSale }
            if (recalced.any { it.targetSale != oldTargets[it.monthYear] }) {
                summaryDao.insertSummaries(recalced)
                list = recalced
            }
            // Index under both the document id and the stored monthYear field: they follow the
            // same "MonthName - yyyy" convention, but indexing both means a row whose field
            // drifted from its id is still findable.
            val byMonth = mutableMapOf<String, SummaryEntity>()
            list.forEach { e ->
                byMonth[e.monthId] = e
                byMonth.putIfAbsent(e.monthYear, e)
            }
            byShop[id] = byMonth
        }
        salesByShop = byShop
    }

    /**
     * Firestore refresh of the selected period. The months come from the calendar
     * ([monthKeysFor]), not from what Room already holds — otherwise a month that was never
     * cached could never be fetched, and would stay permanently blank.
     *
     * The previous window is refreshed too because the purchase target is derived from it.
     * Requests are issued concurrently: a 6-month range across several shops and regions is
     * dozens of round trips, and awaiting them one at a time is what made the screen look stuck.
     */
    private suspend fun refreshSelectedPeriod(range: MonthSelection) = coroutineScope {
        val months = (monthKeysFor(range) + previousMonthKeysFor(range)).distinct()
        months.flatMap { monthId ->
            shops.mapNotNull { s -> s.shopId?.let { async { refreshSalesMonth(it, monthId) } } } +
                regions.mapNotNull { r -> r.shopId?.let { async { refreshAccountMonth(it, monthId) } } }
        }.awaitAll()
    }

    /**
     * True when the selected window has a month with no local record, for any shop or region.
     *
     * Shops are checked against both the selected window AND the preceding window of the same
     * length ([previousMonthKeysFor]) — the purchase target's comparison baseline (see
     * [buildShopStats]) — since a month can look "complete" for sales while its purchase
     * breakdown baseline is still unfetched. [salesByShop] doubles as that signal: refreshing a
     * month always writes its sales summary and purchase breakdown together (see
     * [refreshSalesMonth]), so a present sales entry means the purchase side was synced too
     * (whether or not it turned out to have any purchases). Regions don't need the extra window —
     * the previous window's account summaries aren't used anywhere.
     */
    private suspend fun hasMissingMonths(range: MonthSelection): Boolean {
        val keys = monthKeysFor(range) + previousMonthKeysFor(range)
        val salesMissing = shops.any { shop ->
            val byMonth = shop.shopId?.let { salesByShop[it] } ?: return@any true
            keys.any { byMonth[it] == null }
        }
        if (salesMissing) return true
        return regions.any { region ->
            val id = region.shopId ?: return@any true
            monthKeysFor(range).any { accountDao.getAccountSummary(id, it) == null }
        }
    }

    /** Compute shop + account cards for the selected period. Does NOT toggle isLoading. */
    private suspend fun computeStats() {
        val range = _selectedRange.value
        val keys = monthKeysFor(range)
        // Sales first (fast, Room-backed) so the UI paints immediately…
        _shopStats.value = withContext(Dispatchers.IO) {
            shops.map { shop -> buildShopStats(shop, range) }
        }
        // …then the account card(s), aggregated over the SAME months as the sales cards. Reading
        // a single representative month here was why a "Last 3 Months" card showed one month's
        // collection next to three months of sales.
        _accountCards.value = withContext(Dispatchers.IO) {
            regions.map { region ->
                val id = region.shopId
                val months = if (id == null) emptyList()
                else keys.mapNotNull { accountDao.getAccountSummary(id, it)?.toDomain() }
                RegionAccount(
                    region = region,
                    summary = aggregateAccounts(months),
                    monthsCovered = months.size,
                    monthsExpected = keys.size
                )
            }
        }
    }

    /**
     * Combine one window's account summaries into the figure the card shows.
     *
     * Not every field is additive, so they are combined three different ways:
     *  - flows (collection, purchases, expenses, withdrawal, provision, profits) are summed;
     *  - closing balances are point-in-time, so they come from the newest month alone;
     *  - opening balances come from the oldest month, so opening→closing spans the whole window;
     *  - margins are recomputed from the summed totals. Averaging the stored percentages would
     *    weight a quiet month equally with a busy one.
     *
     * [months] is ordered newest→oldest, matching [monthKeysFor].
     */
    private fun aggregateAccounts(months: List<AccountSummary>): AccountSummary? {
        if (months.isEmpty()) return null
        if (months.size == 1) return months.first()

        val newest = months.first()
        val oldest = months.last()
        val collection = months.sumOf { it.totalCollection }
        val gross = months.sumOf { it.grossProfit }
        val net = months.sumOf { it.netProfit }

        return AccountSummary(
            monthYear = newest.monthYear,
            totalCollection = collection,
            totalPurchases = months.sumOf { it.totalPurchases },
            totalExpenses = months.sumOf { it.totalExpenses },
            withdrawal = months.sumOf { it.withdrawal },
            provision = months.sumOf { it.provision },
            grossProfit = gross,
            netProfit = net,
            grossMargin = if (collection > 0) gross / collection * 100.0 else 0.0,
            netProfitMargin = if (collection > 0) net / collection * 100.0 else 0.0,
            outstandingPayments = newest.outstandingPayments,
            cashBalance = newest.cashBalance,
            outstandingBalance = newest.outstandingBalance,
            accountBalance = newest.accountBalance,
            openingCashBalance = oldest.openingCashBalance,
            openingOutstandingBalance = oldest.openingOutstandingBalance,
            openingAccountBalance = oldest.openingAccountBalance,
            lastUpdated = months.maxOf { it.lastUpdated }
        )
    }

    /**
     * The month keys ("MonthName - yyyy", newest→oldest) a selection covers. This is the single
     * source of truth for every window: refresh, sales stats and the account card all resolve
     * against the same list, so the three can never disagree.
     *
     * [YearMonth.now] is read on each call rather than captured at construction, so a
     * long-running process crossing a month boundary reports the new month.
     */
    private fun monthKeysFor(range: MonthSelection): List<String> = when (range) {
        is MonthSelection.Month ->
            listOf(range.option.yearMonth.format(MonthOption.SUMMARY_KEY_FORMATTER))
        is MonthSelection.TrailingRange -> {
            val now = YearMonth.now()
            (1..range.monthCount).map {
                now.minusMonths(it.toLong()).format(MonthOption.SUMMARY_KEY_FORMATTER)
            }
        }
    }

    /**
     * The window immediately preceding [monthKeysFor], of the same length — the comparison
     * baseline the purchase target (previous × 1.10) is derived from.
     */
    private fun previousMonthKeysFor(range: MonthSelection): List<String> = when (range) {
        is MonthSelection.Month ->
            listOf(
                range.option.yearMonth.minusMonths(1)
                    .format(MonthOption.SUMMARY_KEY_FORMATTER)
            )
        is MonthSelection.TrailingRange -> {
            val now = YearMonth.now()
            val n = range.monthCount
            (n + 1..2 * n).map {
                now.minusMonths(it.toLong()).format(MonthOption.SUMMARY_KEY_FORMATTER)
            }
        }
    }

    private suspend fun buildShopStats(shop: ShopListDto, range: MonthSelection): ShopStats {
        val id = shop.shopId
        val keys = monthKeysFor(range)
        val byMonth = id?.let { salesByShop[it] } ?: emptyMap()
        if (id == null || byMonth.isEmpty()) {
            return ShopStats(shop, null, null, 0.0, monthsExpected = keys.size)
        }

        // Only the requested months that actually exist locally — a missing month is reported
        // via monthsCovered, never substituted with a neighbouring one.
        val statsMonths = keys.mapNotNull { byMonth[it] }
        if (statsMonths.isEmpty()) {
            return ShopStats(shop, null, null, 0.0, monthsExpected = keys.size)
        }

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
        val currentAgg = aggregateBreakdown(id, keys)
        val prevAgg = aggregateBreakdownAmounts(id, previousMonthKeysFor(range))
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

        return ShopStats(
            shop, sales, purchase, margin, lastUpdated,
            monthsCovered = statsMonths.size,
            monthsExpected = keys.size
        )
    }

    private suspend fun aggregateBreakdown(
        shopId: String, months: List<String>
    ): Map<Int, PurchaseItem> {
        val agg = mutableMapOf<Int, PurchaseItem>()
        months.forEach { month ->
            fetchPurchaseBreakdown(shopId, month).forEach { item ->
                val existing = agg[item.categoryId]
                agg[item.categoryId] = existing?.copy(totalAmount = existing.totalAmount + item.totalAmount)
                    ?: item
            }
        }
        return agg
    }

    private suspend fun aggregateBreakdownAmounts(
        shopId: String, months: List<String>
    ): Map<Int, Double> {
        val agg = mutableMapOf<Int, Double>()
        months.forEach { month ->
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
        val entities = breakdown.map { m ->
            PurchaseEntity(
                shopId = shopId,
                monthId = monthId,
                categoryId = parseCategoryId(m["categoryId"]),
                categoryName = m["categoryName"] as? String ?: "Uncategorised",
                totalAmount = parseAmount(m["totalAmount"]),
                lastUpdated = System.currentTimeMillis()
            )
        }
        // Delete unconditionally: a month whose breakdown was emptied upstream must clear its
        // cached rows, otherwise the old figures are aggregated forever.
        runCatching {
            purchaseDao.deletePurchasesForMonth(shopId, monthId)
            if (entities.isNotEmpty()) purchaseDao.insertPurchases(entities)
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
        val raw = suspendCoroutine<List<Map<String, Any>>> { cont ->
            salesFirestore.collection("summary").document(shopId)
                .collection("months").document(monthId).get()
                .addOnSuccessListener { doc ->
                    cont.resume(doc.get("purchaseBreakdown") as? List<Map<String, Any>> ?: emptyList())
                }
                .addOnFailureListener { cont.resume(emptyList()) }
        }
        val items = raw.map { m ->
            PurchaseItem(
                categoryId = parseCategoryId(m["categoryId"]),
                categoryName = m["categoryName"] as? String ?: "Uncategorised",
                totalAmount = parseAmount(m["totalAmount"])
            )
        }
        // Persist so the next call (a re-render, a sibling category's aggregation) reads from
        // Room instead of re-hitting Firestore — same pattern refreshSalesMonth (above) already
        // uses for its own writes. Without this the cache-miss path re-fetches on every call
        // until refreshSelectedPeriod happens to run and populate it separately.
        runCatching {
            if (items.isNotEmpty()) {
                purchaseDao.insertPurchases(
                    items.map {
                        PurchaseEntity(
                            shopId = shopId,
                            monthId = monthId,
                            categoryId = it.categoryId,
                            categoryName = it.categoryName,
                            totalAmount = it.totalAmount,
                            lastUpdated = System.currentTimeMillis()
                        )
                    }
                )
            }
        }
        return items
    }

    private fun parseAmount(v: Any?): Double = when (v) {
        is Double -> v
        is Long -> v.toDouble()
        is String -> v.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    /**
     * Firestore hands back whichever numeric type the writer used. Accepting only `Long` meant
     * a category written as a Double or Int fell through to `0`, collapsing every such category
     * into one bucket and summing their amounts together.
     */
    private fun parseCategoryId(v: Any?): Int = when (v) {
        is Int -> v
        is Long -> v.toInt()
        is Double -> v.toInt()
        is Number -> v.toInt()
        is String -> v.toIntOrNull() ?: 0
        else -> 0
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

/**
 * The period label a card shows. When the local records cover fewer months than the selected
 * period asks for, the shortfall is named ("Last 3 Months (2 of 3)") rather than presenting a
 * partial aggregate as if it were the full window.
 */
fun HomeScreenViewModel.ShopStats.periodLabel(rangeLabel: String): String =
    partialPeriodLabel(rangeLabel, monthsCovered, monthsExpected)

fun HomeScreenViewModel.RegionAccount.periodLabel(rangeLabel: String): String =
    partialPeriodLabel(rangeLabel, monthsCovered, monthsExpected)

private fun partialPeriodLabel(rangeLabel: String, covered: Int, expected: Int): String =
    if (expected > 1 && covered in 1 until expected) "$rangeLabel ($covered of $expected)"
    else rangeLabel
