package com.marsa.smarttrackerhub.ui.screens.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


/**
 * Created by Muhammed Shafi on 12/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class HomeScreenViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeScreenViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}