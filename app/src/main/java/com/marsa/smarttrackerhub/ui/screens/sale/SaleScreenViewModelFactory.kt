package com.marsa.smarttrackerhub.ui.screens.sale

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.FirebaseApp


/**
 * Created by Muhammed Shafi on 09/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class SaleScreenViewModelFactory(
    private val application: Application,
    private val firebaseApp: FirebaseApp
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SaleScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SaleScreenViewModel(application, firebaseApp) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
