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
    private val firebaseSmartTracker: FirebaseApp,
    private val firebaseAccountTracker: FirebaseApp
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatementViewModel(firebaseSmartTracker, firebaseAccountTracker) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
