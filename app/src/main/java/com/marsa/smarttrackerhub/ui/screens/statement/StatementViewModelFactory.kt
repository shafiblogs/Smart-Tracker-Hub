package com.marsa.smarttrackerhub.ui.screens.statement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.FirebaseApp


/**
 * Created by Muhammed Shafi on 09/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class StatementViewModelFactory(
    private val firebaseApp: FirebaseApp
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShopsViewModel(firebaseApp) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
