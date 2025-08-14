package com.marsa.smarttrackerhub.ui.screens.investers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.repository.InvestorRepository
import com.marsa.smarttrackerhub.data.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class InvestorListViewModel() : ViewModel() {
    private val _uiState = MutableStateFlow(InvestorListUiState(isLoading = true))
    val uiState: StateFlow<InvestorListUiState> = _uiState

    private lateinit var repository: InvestorRepository

    fun initDatabase(context: Context) {
        val db = AppDatabase.getDatabase(context)
        repository = InvestorRepository(db.investorDao())
        loadInvestors()
    }

    private fun loadInvestors() = viewModelScope.launch {
        repository.getAllInvestors().collect { list ->
            _uiState.value = InvestorListUiState(investors = list, isLoading = false)
        }
    }
}