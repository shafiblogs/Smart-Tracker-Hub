package com.marsa.smarttrackerhub.ui.screens.summary

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.domain.AccountSummary
import com.marsa.smarttrackerhub.domain.getSummaryListUser
import com.marsa.smarttrackerhub.ui.screens.statement.ShopListDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


/**
 * Created by Muhammed Shafi on 31/05/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class SummaryViewModel(firebaseApp: FirebaseApp) : ViewModel() {

    private val _shops = MutableStateFlow<List<ShopListDto>>(emptyList())
    val shops: StateFlow<List<ShopListDto>> = _shops

    private val _summaries = MutableStateFlow<Map<String, List<AccountSummary>>>(emptyMap())
    val summaries: StateFlow<Map<String, List<AccountSummary>>> = _summaries

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
                Log.e("SummaryViewModel", "Failed to fetch shops", e)
            }
    }*/


    fun loadScreenData(userAccessCode: AccessCode) {
        _shops.value = getSummaryListUser(userAccessCode)
        _shops.value.forEach { shop ->
            shop.shopId?.let { shopId ->
                loadAllSummariesForShop(shopId)
            }
        }
    }

    private fun loadAllSummariesForShop(shopId: String) {
        trackerFireStore.collection("summary")
            .document("USER_ACCOUNT")
            .collection("months")
            .orderBy("monthYear", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SummaryViewModel", "Error fetching summaries for $shopId", error)
                    return@addSnapshotListener
                }

                val allSummaries = snapshot?.documents
                    ?.mapNotNull { it.toObject(AccountSummary::class.java) }
                    .orEmpty()

                _summaries.value = _summaries.value.toMutableMap().apply {
                    put(shopId, allSummaries)
                }

                Log.d(
                    "SummaryViewModel",
                    "Loaded ${allSummaries.size} summaries for $shopId"
                )
            }
    }
}
