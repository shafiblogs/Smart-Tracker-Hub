package com.marsa.smarttrackerhub.ui.screens.logs

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.EmployeeInfo
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "##LogsViewModel"

// ── Selection ────────────────────────────────────────────────────────────────

sealed class SelectionItem {
    data class ShopItem(val shop: ShopInfo) : SelectionItem()
    data class EmployeeItem(val employee: EmployeeInfo, val shopName: String) : SelectionItem()
}

fun SelectionItem.displayLabel(): String = when (this) {
    is SelectionItem.ShopItem     -> shop.shopName
    is SelectionItem.EmployeeItem -> "${employee.employeeName} · $shopName"
}

// ── Month picker ─────────────────────────────────────────────────────────────

data class MonthOption(
    val key: String,         // "yyyy-MM"
    val displayName: String  // "March 2026"
)

// ── Raw log entry ─────────────────────────────────────────────────────────────

data class LogEntry(
    val date:         String = "",
    val eventType:    String = "",   // SHOP_OPEN | SHOP_CLOSE | EMPLOYEE_LOGIN | EMPLOYEE_LOGOUT
    val timestamp:    Long   = 0L,
    val employeeId:   String = "",
    val employeeName: String = ""
)

// ── Shop view data ────────────────────────────────────────────────────────────

data class EmployeeSession(
    val loginTime:       Long,
    val logoutTime:      Long?,
    val durationMinutes: Long
)

data class DayLogSummary(
    val date:             String,
    val shopOpenTime:     Long?,
    val shopCloseTime:    Long?,
    val shopTotalMinutes: Long?,
    val isShopOpen:       Boolean
)

data class ShopMonthSummary(
    val monthDisplay:     String,
    val shopName:         String,
    val totalDaysOpen:    Int,
    val totalMinutes:     Long,
    val avgMinutesPerDay: Long
)

// ── Employee view data ────────────────────────────────────────────────────────

data class EmployeeDayRecord(
    val date:         String,
    val sessions:     List<EmployeeSession>,
    val totalMinutes: Long
)

data class EmployeeMonthSummary(
    val monthDisplay:     String,
    val employeeName:     String,
    val shopName:         String,
    val totalDays:        Int,
    val totalMinutes:     Long,
    val avgMinutesPerDay: Long
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class LogsViewModel : ViewModel() {

    private var db: AppDatabase? = null

    // ── Selection ─────────────────────────────────────────────────────────
    private val _selectionItems = MutableStateFlow<List<SelectionItem>>(emptyList())
    val selectionItems: StateFlow<List<SelectionItem>> = _selectionItems.asStateFlow()

    private val _selectedItem = MutableStateFlow<SelectionItem?>(null)
    val selectedItem: StateFlow<SelectionItem?> = _selectedItem.asStateFlow()

    // ── Month picker ──────────────────────────────────────────────────────
    val availableMonths: List<MonthOption> = buildMonthOptions()

    private val _selectedMonth = MutableStateFlow(availableMonths.first())
    val selectedMonth: StateFlow<MonthOption> = _selectedMonth.asStateFlow()

    // ── Raw logs ──────────────────────────────────────────────────────────
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())

    // ── Shop derived state ────────────────────────────────────────────────
    val shopDaySummaries: StateFlow<List<DayLogSummary>> = combine(
        _logs, _selectedItem
    ) { logs, item ->
        if (item is SelectionItem.ShopItem) computeShopDays(logs) else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val shopMonthSummary: StateFlow<ShopMonthSummary?> = combine(
        _logs, _selectedItem, _selectedMonth
    ) { logs, item, month ->
        if (item is SelectionItem.ShopItem)
            buildShopMonthSummary(logs, item.shop.shopName, month.displayName)
        else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Employee derived state ────────────────────────────────────────────
    val employeeDayRecords: StateFlow<List<EmployeeDayRecord>> = combine(
        _logs, _selectedItem
    ) { logs, item ->
        if (item is SelectionItem.EmployeeItem)
            computeEmployeeDays(logs, item.employee.employeeId)
        else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val employeeMonthSummary: StateFlow<EmployeeMonthSummary?> = combine(
        _logs, _selectedItem, _selectedMonth
    ) { logs, item, month ->
        if (item is SelectionItem.EmployeeItem)
            buildEmployeeMonthSummary(logs, item.employee, item.shopName, month.displayName)
        else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Loading ───────────────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Init ──────────────────────────────────────────────────────────────

    fun initDatabase(context: Context) {
        if (db == null) db = AppDatabase.getDatabase(context)
        loadSelectionList()
    }

    private fun loadSelectionList() = viewModelScope.launch {
        val shops     = db?.shopDao()?.getActiveShopsAsList() ?: emptyList()
        val employees = db?.employeeDao()?.getAllEmployeesAsList()
            ?.filter { it.isActive } ?: emptyList()

        val shopMap = shops.associateBy { it.shopId }
        val items = buildList {
            shops.forEach { add(SelectionItem.ShopItem(it)) }
            employees.forEach { emp ->
                val shopName = shopMap[emp.associatedShopFirebaseId]?.shopName
                    ?: emp.associatedShopFirebaseId
                add(SelectionItem.EmployeeItem(emp, shopName))
            }
        }
        _selectionItems.value = items
    }

    // ── Actions ───────────────────────────────────────────────────────────

    fun selectItem(item: SelectionItem) {
        _selectedItem.value = item
        _logs.value = emptyList()
        loadLogs()
    }

    fun selectMonth(month: MonthOption) {
        _selectedMonth.value = month
        _logs.value = emptyList()
        loadLogs()
    }

    fun refresh() {
        _logs.value = emptyList()
        loadLogs()
    }

    // ── Firebase ──────────────────────────────────────────────────────────

    private fun loadLogs() = viewModelScope.launch {
        val item  = _selectedItem.value ?: return@launch
        val month = _selectedMonth.value.key
        val shopFirebaseId = when (item) {
            is SelectionItem.ShopItem     -> item.shop.shopId
            is SelectionItem.EmployeeItem -> item.employee.associatedShopFirebaseId
        }
        if (shopFirebaseId.isBlank()) return@launch
        _isLoading.value = true
        _logs.value = fetchFromFirestore(shopFirebaseId, month)
        _isLoading.value = false
    }

    private suspend fun fetchFromFirestore(shopFirebaseId: String, monthKey: String): List<LogEntry> {
        val app = runCatching { FirebaseApp.getInstance("SmartTrackerApp") }.getOrNull()
            ?: return emptyList<LogEntry>().also { Log.e(TAG, "SmartTrackerApp not found") }

        val signedIn = suspendCoroutine<Boolean> { cont ->
            FirebaseAuth.getInstance(app).signInAnonymously()
                .addOnSuccessListener { cont.resume(true) }
                .addOnFailureListener { cont.resume(false) }
        }
        if (!signedIn) return emptyList()

        return suspendCoroutine { cont ->
            FirebaseFirestore.getInstance(app)
                .collection("shops").document(shopFirebaseId)
                .collection("logs").document(monthKey)
                .collection("data")
                .get()
                .addOnSuccessListener { snapshot ->
                    val entries = snapshot.documents.mapNotNull { doc ->
                        runCatching {
                            LogEntry(
                                date         = doc.getString("date")         ?: "",
                                eventType    = doc.getString("eventType")    ?: "",
                                timestamp    = doc.getLong("timestamp")      ?: 0L,
                                employeeId   = doc.getString("employeeId")   ?: "",
                                employeeName = doc.getString("employeeName") ?: ""
                            )
                        }.getOrNull()
                    }
                    Log.d(TAG, "Fetched ${entries.size} logs: $shopFirebaseId / $monthKey")
                    cont.resume(entries)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Fetch failed: ${e.message}")
                    cont.resume(emptyList())
                }
        }
    }

    // ── Shop computation ──────────────────────────────────────────────────

    private fun computeShopDays(logs: List<LogEntry>): List<DayLogSummary> {
        return logs
            .filter { it.eventType == "SHOP_OPEN" || it.eventType == "SHOP_CLOSE" }
            .groupBy { it.date }
            .entries.sortedByDescending { it.key }
            .map { (date, dayLogs) ->
                val open  = dayLogs.filter { it.eventType == "SHOP_OPEN"  }.minByOrNull { it.timestamp }
                val close = dayLogs.filter { it.eventType == "SHOP_CLOSE" }.maxByOrNull { it.timestamp }
                val last  = dayLogs.maxByOrNull { it.timestamp }
                val total = if (open != null && close != null)
                    (close.timestamp - open.timestamp) / 60_000L else null
                DayLogSummary(date, open?.timestamp, close?.timestamp, total, last?.eventType == "SHOP_OPEN")
            }
    }

    private fun buildShopMonthSummary(
        logs: List<LogEntry>, shopName: String, monthDisplay: String
    ): ShopMonthSummary {
        val days     = computeShopDays(logs)
        val openDays = days.filter { it.shopOpenTime != null }
        val total    = openDays.sumOf { it.shopTotalMinutes ?: 0L }
        val avg      = if (openDays.isNotEmpty()) total / openDays.size else 0L
        return ShopMonthSummary(monthDisplay, shopName, openDays.size, total, avg)
    }

    // ── Employee computation ──────────────────────────────────────────────

    private fun computeEmployeeDays(logs: List<LogEntry>, employeeId: String): List<EmployeeDayRecord> {
        return logs
            .filter { (it.eventType == "EMPLOYEE_LOGIN" || it.eventType == "EMPLOYEE_LOGOUT") && it.employeeId == employeeId }
            .groupBy { it.date }
            .entries.sortedByDescending { it.key }
            .map { (date, dayLogs) ->
                val sessions = buildSessions(dayLogs.sortedBy { it.timestamp })
                EmployeeDayRecord(date, sessions, sessions.sumOf { it.durationMinutes })
            }
    }

    private fun buildEmployeeMonthSummary(
        logs: List<LogEntry>, employee: EmployeeInfo, shopName: String, monthDisplay: String
    ): EmployeeMonthSummary {
        val days   = computeEmployeeDays(logs, employee.employeeId)
        val worked = days.filter { it.sessions.isNotEmpty() }
        val total  = worked.sumOf { it.totalMinutes }
        val avg    = if (worked.isNotEmpty()) total / worked.size else 0L
        return EmployeeMonthSummary(monthDisplay, employee.employeeName, shopName, worked.size, total, avg)
    }

    private fun buildSessions(sorted: List<LogEntry>): List<EmployeeSession> {
        val sessions = mutableListOf<EmployeeSession>()
        var i = 0
        while (i < sorted.size) {
            val cur = sorted[i]
            if (cur.eventType == "EMPLOYEE_LOGIN") {
                val nxt = sorted.getOrNull(i + 1)
                if (nxt?.eventType == "EMPLOYEE_LOGOUT") {
                    sessions += EmployeeSession(cur.timestamp, nxt.timestamp, (nxt.timestamp - cur.timestamp) / 60_000L)
                    i += 2
                } else {
                    sessions += EmployeeSession(cur.timestamp, null, 0L)
                    i++
                }
            } else i++
        }
        return sessions
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun buildMonthOptions(): List<MonthOption> {
    val now = LocalDate.now()
    val fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    return (0..11).map { ago ->
        val d = now.minusMonths(ago.toLong())
        MonthOption(d.format(DateTimeFormatter.ofPattern("yyyy-MM")), d.format(fmt))
    }
}

fun Long.toHoursLabel(): String {
    val h = this / 60; val m = this % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
