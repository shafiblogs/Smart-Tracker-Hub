package com.marsa.smarttrackerhub.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


/**
 * Created by Muhammed Shafi on 12/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class SalesChartViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SalesChartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SalesChartViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}