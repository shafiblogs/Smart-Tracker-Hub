package com.marsa.smarttrackerhub.ui.screens.summary

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.toDomain
import com.marsa.smarttrackerhub.data.entity.toEntity
import com.marsa.smarttrackerhub.domain.AccountSummary
import com.marsa.smarttrackerhub.domain.AvailableMonth
import com.marsa.smarttrackerhub.ui.components.percentChange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Loads one region's month list + a single month's Account summary (balances + profit) from the
 * AccountsTracker Firebase `summary/{shopId}/months/{monthId}` doc (Room-first, Firestore
 * fallback). Powers the Account month-detail screen: month switcher, refresh, and share.
 */
class AccountDetailViewModel(
    application: Application,
    private val firebaseApp: FirebaseApp,
    private val shopId: String,
    initialMonthId: String
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val accountSummaryDao = db.accountSummaryDao()
    private val firestore = FirebaseFirestore.getInstance(firebaseApp)
    private var monthsListener: ListenerRegistration? = null

    private val _availableMonths = MutableStateFlow<List<AvailableMonth>>(emptyList())
    val availableMonths: StateFlow<List<AvailableMonth>> = _availableMonths

    private val _selectedMonthId = MutableStateFlow(initialMonthId)
    val selectedMonthId: StateFlow<String> = _selectedMonthId

    private val _summary = MutableStateFlow<AccountSummary?>(null)
    val summary: StateFlow<AccountSummary?> = _summary

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /** Month-over-month comparison vs the next-older month (null until computed). */
    private val _comparison = MutableStateFlow<AccountComparison?>(null)
    val comparison: StateFlow<AccountComparison?> = _comparison

    /** Cached month history (newest→oldest, capped at 6) — feeds the profit trend chart. */
    private val _history = MutableStateFlow<List<AccountSummary>>(emptyList())
    val history: StateFlow<List<AccountSummary>> = _history

    init {
        loadMonths()
        loadMonth(_selectedMonthId.value)
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { accountSummaryDao.getAllAccountSummariesForShop(shopId) }
                .getOrNull()
                ?.take(6)
                ?.map { it.toDomain() }
                ?.let { _history.value = it }
        }
    }

    private fun loadMonths() {
        monthsListener = firestore.collection("summary").document(shopId).collection("months")
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                _availableMonths.value = snap?.documents
                    ?.map { doc -> AvailableMonth(id = doc.id) }
                    .orEmpty()
                    .sortedByDescending { it.yearMonth?.let { ym -> ym.year * 12L + ym.monthValue } ?: Long.MIN_VALUE }
                updateComparison()
            }
    }

    fun selectMonth(monthId: String) {
        if (monthId == _selectedMonthId.value && _summary.value != null) return
        _selectedMonthId.value = monthId
        loadMonth(monthId)
    }

    /** Older month (list is newest→oldest, so previous = index + 1). */
    fun goOlder() = move(1)

    /** Newer month. */
    fun goNewer() = move(-1)

    private fun move(delta: Int) {
        val months = _availableMonths.value
        val idx = months.indexOfFirst { it.id == _selectedMonthId.value }
        if (idx < 0) return
        (idx + delta).takeIf { it in months.indices }?.let { selectMonth(months[it].id) }
    }

    fun refresh() {
        val monthId = _selectedMonthId.value
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { accountSummaryDao.deleteAccountSummary(shopId, monthId) }
            loadFromFirestore(monthId)
        }
    }

    private fun loadMonth(monthId: String) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val cached = accountSummaryDao.getAccountSummary(shopId, monthId)
            if (cached != null) {
                _summary.value = cached.toDomain()
                _isLoading.value = false
                updateComparison()
            } else {
                loadFromFirestore(monthId)
            }
        }
    }

    private fun loadFromFirestore(monthId: String) {
        firestore.collection("summary").document(shopId)
            .collection("months").document(monthId)
            .get()
            .addOnSuccessListener { doc ->
                val s = doc.toObject(AccountSummary::class.java)
                    ?.let { if (it.lastUpdated == 0L) it.copy(lastUpdated = System.currentTimeMillis()) else it }
                if (s != null) {
                    _summary.value = s
                    updateComparison()
                    viewModelScope.launch(Dispatchers.IO) {
                        runCatching { accountSummaryDao.insertAccountSummary(s.toEntity(shopId, monthId)) }
                        loadHistory()
                    }
                }
                _isLoading.value = false
            }
            .addOnFailureListener {
                Log.e("AccountDetailVM", "load $monthId failed", it)
                _isLoading.value = false
            }
    }

    /** Recompute the vs-previous-month deltas for the currently selected month. */
    private fun updateComparison() {
        val current = _summary.value
        val months = _availableMonths.value
        val idx = months.indexOfFirst { it.id == _selectedMonthId.value }
        val prev = months.getOrNull(idx + 1)
        if (current == null || idx < 0 || prev == null) {
            _comparison.value = null
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val p = fetchAccountDomain(prev.id)
            _comparison.value = AccountComparison(
                referenceLabel = prev.displayName,
                netProfitDeltaPct = p?.let { percentChange(current.netProfit, it.netProfit) },
                cashBalanceDeltaPct = p?.let { percentChange(current.cashBalance, it.cashBalance) }
            )
        }
    }

    /** Room-first, Firestore-fallback fetch of one month's account summary (for comparison only). */
    private suspend fun fetchAccountDomain(monthId: String): AccountSummary? {
        accountSummaryDao.getAccountSummary(shopId, monthId)?.let { return it.toDomain() }
        return suspendCoroutine { cont ->
            firestore.collection("summary").document(shopId)
                .collection("months").document(monthId).get()
                .addOnSuccessListener { cont.resume(it.toObject(AccountSummary::class.java)) }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        monthsListener?.remove()
    }
}

/** Account month-over-month comparison (vs the next-older month). */
data class AccountComparison(
    val referenceLabel: String,
    val netProfitDeltaPct: Double?,
    val cashBalanceDeltaPct: Double?
)

class AccountDetailViewModelFactory(
    private val application: Application,
    private val firebaseApp: FirebaseApp,
    private val shopId: String,
    private val monthId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountDetailViewModel::class.java)) {
            return AccountDetailViewModel(application, firebaseApp, shopId, monthId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
