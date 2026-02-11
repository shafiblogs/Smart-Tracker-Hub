package com.marsa.smarttrackerhub.ui.screens.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.toDomain
import com.marsa.smarttrackerhub.data.entity.toEntity
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.domain.MonthlySummary
import com.marsa.smarttrackerhub.domain.getHomeShopUser
import com.marsa.smarttrackerhub.ui.screens.statement.ShopListDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeScreenViewModel(
    application: Application,
    firebaseApp: FirebaseApp
) : AndroidViewModel(application) {

    private val _shops = MutableStateFlow<List<ShopListDto>>(emptyList())
    val shops: StateFlow<List<ShopListDto>> = _shops

    private val _availableMonths = MutableStateFlow<List<MonthItem>>(emptyList())
    val availableMonths: StateFlow<List<MonthItem>> = _availableMonths

    private val _summariesCache = MutableStateFlow<Map<String, MonthlySummary>>(emptyMap())
    val summariesCache: StateFlow<Map<String, MonthlySummary>> = _summariesCache

    private val trackerFireStore = FirebaseFirestore.getInstance(firebaseApp)
    private val database = AppDatabase.getDatabase(application)
    private val summaryDao = database.summaryDao()

    private val _selectedShop = MutableStateFlow<ShopListDto?>(null)
    val selectedShop: StateFlow<ShopListDto?> = _selectedShop

    private val _selectedMonthId = MutableStateFlow<String?>(null)
    val selectedMonthId: StateFlow<String?> = _selectedMonthId

    private val _expanded = MutableStateFlow(false)
    val expanded: StateFlow<Boolean> = _expanded

    private val _isLoadingMonth = MutableStateFlow(false)
    val isLoadingMonth: StateFlow<Boolean> = _isLoadingMonth

    private var monthsListenerRegistration: ListenerRegistration? = null

    // Cache expiry time (e.g., 24 hours)
    private val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L

    fun setSelectedShop(shop: ShopListDto?) {
        monthsListenerRegistration?.remove()

        _selectedShop.value = shop
        _selectedMonthId.value = null
        _availableMonths.value = emptyList()
        _summariesCache.value = emptyMap()

        shop?.shopId?.let { shopId ->
            loadMonthListForShop(shopId)
        }
    }

    fun selectMonth(monthId: String) {
        _selectedMonthId.value = monthId

        val shopId = _selectedShop.value?.shopId
        if (shopId != null) {
            if (!_summariesCache.value.containsKey(monthId)) {
                loadSummaryForMonth(shopId, monthId)
            }
        }
    }

    fun setExpanded(value: Boolean) {
        _expanded.value = value
    }

    fun loadScreenData(userAccessCode: AccessCode) {
        _shops.value = getHomeShopUser(userAccessCode)

        if (_shops.value.isNotEmpty()) {
            setSelectedShop(_shops.value.first())
        }

        // Clean up old cache on app start
        viewModelScope.launch(Dispatchers.IO) {
            val expiryTime = System.currentTimeMillis() - CACHE_EXPIRY_MS
            val currentMonthId = getCurrentMonthId()
            summaryDao.deleteExpiredCurrentMonth(
                currentMonthId = currentMonthId,
                expiryTime = expiryTime
            )
        }
    }

    private fun getCurrentMonthId(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return "%04d-%02d".format(year, month)
    }

    private fun loadMonthListForShop(shopId: String) {
        monthsListenerRegistration = trackerFireStore.collection("summary")
            .document(shopId)
            .collection("months")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HomeViewModel", "Error fetching months for $shopId", error)
                    return@addSnapshotListener
                }

                val monthsList = snapshot?.documents
                    ?.map { doc ->
                        MonthItem(
                            id = doc.id,
                            displayName = doc.id
                        )
                    }
                    .orEmpty()
                    .sortedByDescending { monthItem ->
                        parseMonthYear(monthItem.id)
                    }

                _availableMonths.value = monthsList

                Log.d("HomeViewModel", "Loaded ${monthsList.size} month IDs for $shopId")
            }
    }

    private fun loadSummaryForMonth(shopId: String, monthId: String) {
        _isLoadingMonth.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // First, try to load from local database
                val cachedEntity = summaryDao.getSummary(shopId, monthId)

                if (cachedEntity != null && !isCacheExpired(cachedEntity.lastUpdated)) {
                    // Use cached data
                    val summary = cachedEntity.toDomain()
                    _summariesCache.value = _summariesCache.value.toMutableMap().apply {
                        put(monthId, summary)
                    }
                    _isLoadingMonth.value = false
                    Log.d("HomeViewModel", "Loaded summary from cache for $shopId - $monthId")
                } else {
                    // Load from Firestore
                    loadFromFirestore(shopId, monthId)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading from cache", e)
                loadFromFirestore(shopId, monthId)
            }
        }
    }

    private fun loadFromFirestore(shopId: String, monthId: String) {
        trackerFireStore.collection("summary")
            .document(shopId)
            .collection("months")
            .document(monthId)
            .get()
            .addOnSuccessListener { document ->
                val summary = document.toObject(MonthlySummary::class.java)

                if (summary != null) {
                    // Add to in-memory cache
                    _summariesCache.value = _summariesCache.value.toMutableMap().apply {
                        put(monthId, summary)
                    }

                    // Save to local database
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val entity = summary.toEntity(shopId, monthId)
                            summaryDao.insertSummary(entity)
                            Log.d(
                                "HomeViewModel",
                                "Saved summary to local DB for $shopId - $monthId"
                            )
                        } catch (e: Exception) {
                            Log.e("HomeViewModel", "Error saving to local DB", e)
                        }
                    }

                    Log.d("HomeViewModel", "Loaded summary from Firestore for $shopId - $monthId")
                }

                _isLoadingMonth.value = false
            }
            .addOnFailureListener { error ->
                Log.e("HomeViewModel", "Error fetching month $monthId", error)
                _isLoadingMonth.value = false
            }
    }

    private fun isCacheExpired(lastUpdated: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastUpdated) > CACHE_EXPIRY_MS
    }

    private fun parseMonthYear(monthYear: String): Long {
        return try {
            val formatter = SimpleDateFormat("MMMM-yyyy", Locale.ENGLISH)
            formatter.parse(monthYear)?.time ?: 0L
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error parsing monthYear: $monthYear", e)
            0L
        }
    }

    override fun onCleared() {
        super.onCleared()
        monthsListenerRegistration?.remove()
    }
}

data class MonthItem(
    val id: String,
    val displayName: String
)