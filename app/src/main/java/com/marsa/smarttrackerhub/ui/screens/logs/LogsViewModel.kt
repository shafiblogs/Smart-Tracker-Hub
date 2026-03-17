package com.marsa.smarttrackerhub.ui.screens.logs

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * Created by Muhammed Shafi on 17/03/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 *
 * Reads shop-activity log events (employee login/logout, shop open/close)
 * from SmartTracker's Firebase ("SmartTrackerApp" secondary instance),
 * groups them as day-level summaries for display.
 */

// ── Log entry (mirrors SmartTracker's Firestore document) ────────────────────

data class LogEntry(
    val date:         String = "",
    val eventType:    String = "",   // SHOP_OPEN | SHOP_CLOSE | EMPLOYEE_LOGIN | EMPLOYEE_LOGOUT
    val timestamp:    Long   = 0L,
    val employeeId:   String = "",
    val employeeName: String = ""
)

// ── Per-day summary data classes ─────────────────────────────────────────────

data class EmployeeSession(
    val loginTime:       Long,
    val logoutTime:      Long?,   // null = still working
    val durationMinutes: Long
)

data class EmployeeLogSummary(
    val employeeId:   String,
    val employeeName: String,
    val sessions:     List<EmployeeSession>,
    val totalMinutes: Long
)

data class DayLogSummary(
    val date:                String,
    val shopOpenTime:        Long?,   // null = not opened
    val shopCloseTime:       Long?,   // null = not closed yet
    val shopTotalMinutes:    Long?,
    val isShopCurrentlyOpen: Boolean,
    val employees:           List<EmployeeLogSummary>
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class LogsViewModel : ViewModel() {

    private val tag = "##LogsViewModel"

    // ── Shop list ─────────────────────────────────────────────────────────
    private val _shopList = MutableStateFlow<List<ShopInfo>>(emptyList())
    val shopList: StateFlow<List<ShopInfo>> = _shopList.asStateFlow()

    private val _selectedShop = MutableStateFlow<ShopInfo?>(null)
    val selectedShop: StateFlow<ShopInfo?> = _selectedShop.asStateFlow()

    // ── Raw log entries ───────────────────────────────────────────────────
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())

    /** Day-level summaries, recomputed whenever logs change. */
    val daySummaries: StateFlow<List<DayLogSummary>> = _logs
        .map { logs -> computeDaySummaries(logs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── State ─────────────────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Current month key "yyyy-MM" (e.g. "2026-03"). */
    val currentMonthKey: String = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("yyyy-MM"))

    private var db: AppDatabase? = null

    // ── Init ──────────────────────────────────────────────────────────────

    fun initDatabase(context: Context) {
        if (db == null) db = AppDatabase.getDatabase(context)
        loadShops()
    }

    private fun loadShops() = viewModelScope.launch {
        _shopList.value = db?.shopDao()?.getAllShopsAsList() ?: emptyList()
    }

    // ── Actions ───────────────────────────────────────────────────────────

    fun selectShop(shop: ShopInfo) {
        _selectedShop.value = shop
        loadCurrentMonthLogs(shop.shopId)
    }

    private fun loadCurrentMonthLogs(shopFirebaseId: String) = viewModelScope.launch {
        _isLoading.value = true
        _logs.value = fetchLogs(shopFirebaseId, currentMonthKey)
        _isLoading.value = false
    }

    fun refresh() = viewModelScope.launch {
        _isLoading.value = true
        val shop = _selectedShop.value
        if (shop != null) {
            _logs.value = fetchLogs(shop.shopId, currentMonthKey)
        }
        _isLoading.value = false
    }

    // ── Firebase fetch ────────────────────────────────────────────────────

    /**
     * Fetches logs from SmartTracker's Firebase.
     * Path: shops/{shopFirebaseId}/logs/{monthKey}/data
     */
    private suspend fun fetchLogs(shopFirebaseId: String, monthKey: String): List<LogEntry> {
        val app = runCatching { FirebaseApp.getInstance("SmartTrackerApp") }.getOrNull()
        if (app == null) {
            Log.e(tag, "SmartTrackerApp Firebase instance not found")
            return emptyList()
        }

        val auth = FirebaseAuth.getInstance(app)
        val signedIn = suspendCoroutine<Boolean> { cont ->
            auth.signInAnonymously()
                .addOnSuccessListener { cont.resume(true) }
                .addOnFailureListener { e ->
                    Log.e(tag, "SmartTrackerApp sign-in failed: ${e.message}")
                    cont.resume(false)
                }
        }
        if (!signedIn) return emptyList()

        val firestore = FirebaseFirestore.getInstance(app)
        return suspendCoroutine { cont ->
            firestore.collection("shops")
                .document(shopFirebaseId)
                .collection("logs")
                .document(monthKey)
                .collection("data")
                .get()
                .addOnSuccessListener { snapshot ->
                    val entries = snapshot.documents.mapNotNull { doc ->
                        try {
                            LogEntry(
                                date         = doc.getString("date")         ?: "",
                                eventType    = doc.getString("eventType")    ?: "",
                                timestamp    = doc.getLong("timestamp")      ?: 0L,
                                employeeId   = doc.getString("employeeId")   ?: "",
                                employeeName = doc.getString("employeeName") ?: ""
                            )
                        } catch (e: Exception) {
                            Log.w(tag, "Failed to parse log doc ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    Log.d(tag, "Fetched ${entries.size} log entries for $shopFirebaseId / $monthKey")
                    cont.resume(entries)
                }
                .addOnFailureListener { e ->
                    Log.e(tag, "Failed to fetch logs: ${e.message}")
                    cont.resume(emptyList())
                }
        }
    }

    // ── Day-summary computation ───────────────────────────────────────────

    private fun computeDaySummaries(logs: List<LogEntry>): List<DayLogSummary> {
        val byDate = logs.groupBy { it.date }
        return byDate.entries
            .sortedByDescending { it.key }
            .map { (date, dayLogs) ->
                // Shop open / close
                val shopOpen  = dayLogs.filter { it.eventType == "SHOP_OPEN"  }.minByOrNull { it.timestamp }
                val shopClose = dayLogs.filter { it.eventType == "SHOP_CLOSE" }.maxByOrNull { it.timestamp }
                val lastShopEvt = dayLogs
                    .filter { it.eventType == "SHOP_OPEN" || it.eventType == "SHOP_CLOSE" }
                    .maxByOrNull { it.timestamp }
                val isOpen    = lastShopEvt?.eventType == "SHOP_OPEN"
                val shopTotal = if (shopOpen != null && shopClose != null)
                    (shopClose.timestamp - shopOpen.timestamp) / 60_000L else null

                // Employee sessions
                val empLogs = dayLogs.filter {
                    it.eventType == "EMPLOYEE_LOGIN" || it.eventType == "EMPLOYEE_LOGOUT"
                }
                val employees = empLogs.groupBy { it.employeeId }
                    .map { (_, eLogs) ->
                        val sorted   = eLogs.sortedBy { it.timestamp }
                        val sessions = buildSessions(sorted)
                        EmployeeLogSummary(
                            employeeId   = sorted.first().employeeId,
                            employeeName = sorted.first().employeeName,
                            sessions     = sessions,
                            totalMinutes = sessions.sumOf { it.durationMinutes }
                        )
                    }
                    .sortedBy { it.sessions.firstOrNull()?.loginTime ?: Long.MAX_VALUE }

                DayLogSummary(date, shopOpen?.timestamp, shopClose?.timestamp, shopTotal, isOpen, employees)
            }
    }

    private fun buildSessions(sorted: List<LogEntry>): List<EmployeeSession> {
        val sessions = mutableListOf<EmployeeSession>()
        var i = 0
        while (i < sorted.size) {
            val cur = sorted[i]
            if (cur.eventType == "EMPLOYEE_LOGIN") {
                val nxt = sorted.getOrNull(i + 1)
                if (nxt?.eventType == "EMPLOYEE_LOGOUT") {
                    sessions += EmployeeSession(
                        cur.timestamp, nxt.timestamp,
                        (nxt.timestamp - cur.timestamp) / 60_000L
                    )
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
