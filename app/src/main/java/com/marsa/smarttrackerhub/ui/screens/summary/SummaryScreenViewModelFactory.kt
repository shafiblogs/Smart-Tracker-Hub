package com.marsa.smarttrackerhub.ui.screens.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.FirebaseApp


/**
 * Created by Muhammed Shafi on 13/11/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class SummaryScreenViewModelFactory(
    private val firebaseApp: FirebaseApp
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SummaryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SummaryViewModel(firebaseApp) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}