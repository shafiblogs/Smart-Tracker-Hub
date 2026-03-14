package com.marsa.smarttrackerhub.ui.screens.purchase

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.FirebaseApp

/**
 * Created by Muhammed Shafi on 13/03/2026.
 * Moro Hub
 */
class PurchaseScreenViewModelFactory(
    private val application: Application,
    private val firebaseApp: FirebaseApp
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PurchaseScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PurchaseScreenViewModel(application, firebaseApp) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
