package com.marsa.smarttrackerhub.ui.screens.shops

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


/**
 * Created by Muhammed Shafi on 31/05/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class ShopsViewModel : ViewModel() {
    private val _shops = MutableStateFlow<List<Shop>>(emptyList())
    val shops: StateFlow<List<Shop>> = _shops

    private val trackerFireStore = FirebaseFirestore.getInstance(
        FirebaseApp.getInstance("SmartTrackerApp")
    )

    init {
        loadShops()
    }

    private fun loadShops() {
        trackerFireStore.collection("shops")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ShopsViewModel", "Error fetching shops", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val shopList = snapshot.documents.map { doc ->
                        doc.toObject(Shop::class.java)?.copy(shopId = doc.id)
                    }.filterNotNull()

                    _shops.value = shopList
                }
            }
    }
}