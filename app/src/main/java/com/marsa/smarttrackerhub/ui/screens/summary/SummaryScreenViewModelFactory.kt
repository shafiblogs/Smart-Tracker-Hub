package com.marsa.smarttrackerhub.ui.screens.summary

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.FirebaseApp

class SummaryScreenViewModelFactory(
    private val application: Application,
    private val firebaseApp: FirebaseApp
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SummaryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SummaryViewModel(application, firebaseApp) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}