package com.marsa.smarttrackerhub.ui.screens.investers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.repository.InvestorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class InvestorListViewModel(
    private val repository: InvestorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvestorListUiState(isLoading = true))
    val uiState: StateFlow<InvestorListUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.getAllInvestors().collect { list ->
                _uiState.value = InvestorListUiState(investors = list, isLoading = false)
            }
        }
    }
}