package com.marsa.smarttrackerhub.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
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

    init {
        loadSummary()
    }

    private fun loadSummary() {
        trackerFireStore.collection("summary")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HomeViewModel", "Error fetching summary", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    _summary.value = snapshot.documents.mapNotNull {
                        it.toObject(MonthlySummary::class.java)
                    }
                }
            }
    }
}