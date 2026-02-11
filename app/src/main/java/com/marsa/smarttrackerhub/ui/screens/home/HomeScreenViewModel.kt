package com.marsa.smarttrackerhub.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.domain.MonthlySummary
import com.marsa.smarttrackerhub.domain.getHomeShopUser
import com.marsa.smarttrackerhub.ui.screens.statement.ShopListDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Locale

class HomeScreenViewModel(firebaseApp: FirebaseApp) : ViewModel() {

    private val _shops = MutableStateFlow<List<ShopListDto>>(emptyList())
    val shops: StateFlow<List<ShopListDto>> = _shops

    // Store month list instead of full summaries
    private val _availableMonths = MutableStateFlow<List<MonthItem>>(emptyList())
    val availableMonths: StateFlow<List<MonthItem>> = _availableMonths

    // Cache for loaded summaries
    private val _summariesCache = MutableStateFlow<Map<String, MonthlySummary>>(emptyMap())
    val summariesCache: StateFlow<Map<String, MonthlySummary>> = _summariesCache

    private val trackerFireStore = FirebaseFirestore.getInstance(firebaseApp)

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
        // Clean up previous listener
        monthsListenerRegistration?.remove()

        _selectedShop.value = shop
        _selectedMonthId.value = null
        _availableMonths.value = emptyList()
        _summariesCache.value = emptyMap()

        // Load ONLY the month list, not the actual data
        shop?.shopId?.let { shopId ->
            loadMonthListForShop(shopId)
        }
    }

    fun selectMonth(monthId: String) {
        _selectedMonthId.value = monthId

        val shopId = _selectedShop.value?.shopId
        if (shopId != null) {
            // Check if already loaded in cache
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

                // Get month list and sort with current month first
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

        trackerFireStore.collection("summary")
            .document(shopId)
            .collection("months")
            .document(monthId)
            .get()
            .addOnSuccessListener { document ->
                val summary = document.toObject(MonthlySummary::class.java)

                if (summary != null) {
                    // Add to cache
                    _summariesCache.value = _summariesCache.value.toMutableMap().apply {
                        put(monthId, summary)
                    }
                    Log.d("HomeViewModel", "Loaded summary for $shopId - $monthId")
                }

                _isLoadingMonth.value = false
            }
            .addOnFailureListener { error ->
                Log.e("HomeViewModel", "Error fetching month $monthId", error)
                _isLoadingMonth.value = false
            }
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

// Simple data class to represent a month in the list
data class MonthItem(
    val id: String,
    val displayName: String
)