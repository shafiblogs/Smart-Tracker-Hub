package com.marsa.smarttrackerhub.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.marsa.smarttrackerhub.domain.MonthlySummary
import com.marsa.smarttrackerhub.ui.screens.statement.ShopListDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


/**
 * Created by Muhammed Shafi on 14/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

class HomeScreenViewModel(firebaseApp: FirebaseApp) : ViewModel() {
    private val _shops = MutableStateFlow<List<ShopListDto>>(emptyList())
    val shops: StateFlow<List<ShopListDto>> = _shops

    private val _summaries = MutableStateFlow<Map<String, List<MonthlySummary>>>(emptyMap())
    val summaries: StateFlow<Map<String, List<MonthlySummary>>> = _summaries

    private val trackerFireStore = FirebaseFirestore.getInstance(firebaseApp)

    private val _selectedShop = MutableStateFlow<ShopListDto?>(null)
    val selectedShop: StateFlow<ShopListDto?> = _selectedShop

    private val _expanded = MutableStateFlow(false)
    val expanded: StateFlow<Boolean> = _expanded

    fun setSelectedShop(shop: ShopListDto?) {
        _selectedShop.value = shop
    }

    fun setExpanded(value: Boolean) {
        _expanded.value = value
    }

    private val hardcodedShops = listOf(
        ShopListDto(name = "Al Marsa Grocery", address = "Masfout", shopId = "MARSA_102"),
        ShopListDto(name = "Al Marsa Grocery", address = "Muzeira", shopId = "MARSA_101"),
        ShopListDto(name = "AL Wadi Cafe", address = "Muzeira", shopId = "WADI_101")
    )

    /*fun loadScreenData() {
        trackerFireStore.collection("shops")
            .get()
            .addOnSuccessListener { snapshot ->
                val shopIds = snapshot.documents.map { it.id }
                shopIds.forEach { shopId ->
                    loadLatestSummary(shopId)
                }
            }
            .addOnFailureListener { e ->
                Log.e("HomeViewModel", "Failed to fetch shops", e)
            }
    }*/

    fun loadScreenData() {
        _shops.value = hardcodedShops
        hardcodedShops.forEach { shop ->
            loadAllSummariesForShop(shop.shopId!!)
        }
    }

    private fun loadAllSummariesForShop(shopId: String) {
        trackerFireStore.collection("summary")
            .document(shopId)
            .collection("months")
            .orderBy("monthYear", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HomeViewModel", "Error fetching summaries for $shopId", error)
                    return@addSnapshotListener
                }

                val allSummaries = snapshot?.documents
                    ?.mapNotNull { it.toObject(MonthlySummary::class.java) }
                    .orEmpty()

                _summaries.value = _summaries.value.toMutableMap().apply {
                    put(shopId, allSummaries)
                }

                Log.d(
                    "HomeViewModel",
                    "Loaded ${allSummaries.size} summaries for $shopId"
                )
            }
    }
}
