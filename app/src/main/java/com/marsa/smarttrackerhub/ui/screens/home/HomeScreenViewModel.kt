package com.marsa.smarttrackerhub.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.marsa.smarttrackerhub.domain.MonthlySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


/**
 * Created by Muhammed Shafi on 14/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

class HomeScreenViewModel(firebaseApp: FirebaseApp) : ViewModel() {
    private val _summary = MutableStateFlow<List<MonthlySummary>>(emptyList())
    val summary: StateFlow<List<MonthlySummary>> = _summary

    private val trackerFireStore = FirebaseFirestore.getInstance(firebaseApp)

    fun loadScreenData() {
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
    }

    private fun loadLatestSummary(shopId: String) {
        trackerFireStore.collection("summary")
            .document(shopId)
            .collection("months")
            .orderBy("monthYear", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HomeViewModel", "Error fetching summary for $shopId", error)
                    return@addSnapshotListener
                }

                val latestSummary = snapshot?.documents?.firstOrNull()?.toObject(MonthlySummary::class.java)
                latestSummary?.let { summary ->
                    // Merge into current list (replace if shop already exists)
                    val updatedList = _summary.value.toMutableList().apply {
                        removeAll { it.shopId == shopId }
                        add(summary)
                    }
                    _summary.value = updatedList
                    Log.d("HomeViewModel", "Loaded latest summary: ${summary.monthYear} for $shopId")
                }
            }
    }
}
