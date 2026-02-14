package com.marsa.smarttrackerhub.ui.screens.summary

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
import com.marsa.smarttrackerhub.domain.AccountSummary
import com.marsa.smarttrackerhub.domain.getSummaryShopList
import com.marsa.smarttrackerhub.ui.screens.statement.ShopListDto
import com.marsa.smarttrackerhub.ui.screens.sale.TargetSaleCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SummaryViewModel(
    application: Application,
    firebaseApp: FirebaseApp
) : AndroidViewModel(application) {

    private val _shops = MutableStateFlow<List<ShopListDto>>(emptyList())
    val shops: StateFlow<List<ShopListDto>> = _shops

    private val _availableMonths = MutableStateFlow<List<MonthItem>>(emptyList())
    val availableMonths: StateFlow<List<MonthItem>> = _availableMonths

    private val _summariesCache = MutableStateFlow<Map<String, AccountSummary>>(emptyMap())
    val summariesCache: StateFlow<Map<String, AccountSummary>> = _summariesCache

    private val trackerFireStore = FirebaseFirestore.getInstance(firebaseApp)
    private val database = AppDatabase.getDatabase(application)
    private val accountSummaryDao = database.accountSummaryDao()

    private val _selectedShop = MutableStateFlow<ShopListDto?>(null)
    val selectedShop: StateFlow<ShopListDto?> = _selectedShop

    private val _selectedMonthId = MutableStateFlow<String?>(null)
    val selectedMonthId: StateFlow<String?> = _selectedMonthId

    private val _expanded = MutableStateFlow(false)
    val expanded: StateFlow<Boolean> = _expanded

    private val _isLoadingMonth = MutableStateFlow(false)
    val isLoadingMonth: StateFlow<Boolean> = _isLoadingMonth

    private var monthsListenerRegistration: ListenerRegistration? = null

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
        _shops.value = getSummaryShopList(userAccessCode)
    }

    /**
     * Force refresh a specific month from Firestore
     */
    fun refreshMonth(monthId: String) {
        val shopId = _selectedShop.value?.shopId
        if (shopId != null) {
            // Remove from cache to force reload
            _summariesCache.value = _summariesCache.value.toMutableMap().apply {
                remove(monthId)
            }

            // Load fresh data from Firestore
            loadFromFirestore(shopId, monthId)

            Log.d("SummaryViewModel", "Refreshing month $monthId from server")
        }
    }

    private fun loadMonthListForShop(shopId: String) {
        monthsListenerRegistration = trackerFireStore.collection("summary")
            .document(shopId)
            .collection("months")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SummaryViewModel", "Error fetching months for $shopId", error)
                    return@addSnapshotListener
                }

                val monthsList = snapshot?.documents
                    ?.map { doc ->
                        MonthItem(
                            id = doc.id,
                            displayName = doc.id,
                            timestamp = TargetSaleCalculator.parseMonthYearToTimestamp(doc.id)
                        )
                    }
                    .orEmpty()
                    .sortedByDescending { it.timestamp }

                _availableMonths.value = monthsList

                Log.d("SummaryViewModel", "Loaded ${monthsList.size} month IDs for $shopId")
            }
    }

    private fun loadSummaryForMonth(shopId: String, monthId: String) {
        _isLoadingMonth.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Try to load from local database first
                val cachedEntity = accountSummaryDao.getAccountSummary(shopId, monthId)

                if (cachedEntity != null) {
                    // Use cached data
                    val summary = cachedEntity.toDomain()
                    _summariesCache.value = _summariesCache.value.toMutableMap().apply {
                        put(monthId, summary)
                    }
                    _isLoadingMonth.value = false
                    Log.d("SummaryViewModel", "Loaded account summary from cache for $shopId - $monthId")
                } else {
                    // Load from Firestore if not in cache
                    loadFromFirestore(shopId, monthId)
                }
            } catch (e: Exception) {
                Log.e("SummaryViewModel", "Error loading from cache", e)
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
                val summary = document.toObject(AccountSummary::class.java)

                if (summary != null) {
                    // IMPORTANT: Set timestamp BEFORE using the summary
                    val updatedSummary = summary.copy(
                        lastUpdated = System.currentTimeMillis()
                    )

                    // Add to in-memory cache
                    _summariesCache.value = _summariesCache.value.toMutableMap().apply {
                        put(monthId, updatedSummary)
                    }

                    // Save to local database
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val entity = updatedSummary.toEntity(shopId, monthId)
                            accountSummaryDao.insertAccountSummary(entity)

                            Log.d("SummaryViewModel", "Saved account summary to local DB for $shopId - $monthId")
                        } catch (e: Exception) {
                            Log.e("SummaryViewModel", "Error saving to local DB", e)
                        }
                    }

                    Log.d("SummaryViewModel", "Loaded account summary from Firestore for $shopId - $monthId")
                }

                _isLoadingMonth.value = false
            }
            .addOnFailureListener { error ->
                Log.e("SummaryViewModel", "Error fetching month $monthId", error)
                _isLoadingMonth.value = false
            }
    }

    override fun onCleared() {
        super.onCleared()
        monthsListenerRegistration?.remove()
    }
}

data class MonthItem(
    val id: String,
    val displayName: String,
    val timestamp: Long = 0L
)