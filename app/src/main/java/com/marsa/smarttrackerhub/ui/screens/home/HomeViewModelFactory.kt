package com.marsa.smarttrackerhub.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.FirebaseApp


/**
 * Created by Muhammed Shafi on 09/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class HomeViewModelFactory(
    private val firebaseApp: FirebaseApp
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(firebaseApp) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
