package com.marsa.smarttrackerhub.ui.screens.investers

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.InvestorInfo
import com.marsa.smarttrackerhub.data.repository.InvestorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class InvestorAddViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _formState = MutableStateFlow(InvestorFormState())
    val formState: StateFlow<InvestorFormState> = _formState.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val isFormValid: StateFlow<Boolean> = formState
        .map { it.investorName.isNotBlank() && it.investorPhone.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun loadInvestor(context: Context, investorId: Int) = viewModelScope.launch {
        try {
            val db = AppDatabase.getDatabase(context)
            val repo = InvestorRepository(db.investorDao())
            val investor = repo.getInvestorById(investorId)

            investor?.let {
                _formState.value = InvestorFormState(
                    investorId = it.id,
                    investorName = it.investorName,
                    investorEmail = it.investorEmail,
                    investorPhone = it.investorPhone
                )
                _isLoaded.value = true
            }
        } catch (e: Exception) {
            _error.value = "Failed to load investor: ${e.localizedMessage}"
        }
    }

    fun updateName(value: String) {
        _formState.update { it.copy(investorName = value, nameError = null) }
        _error.value = null
    }

    fun updateEmail(value: String) {
        _formState.update { it.copy(investorEmail = value) }
        _error.value = null
    }

    fun updatePhone(value: String) {
        if (value.isEmpty() || value.matches(Regex("^[+\\d\\s\\-()]*$"))) {
            _formState.update { it.copy(investorPhone = value, phoneError = null) }
            _error.value = null
        }
    }

    fun saveInvestor(
        context: Context,
        onSuccess: () -> Unit = {},
        onFail: (String) -> Unit = {}
    ) = viewModelScope.launch {
        val state = _formState.value

        if (!validate(state)) return@launch

        try {
            val db = AppDatabase.getDatabase(context)
            val repo = InvestorRepository(db.investorDao())

            val investor = InvestorInfo(
                id = state.investorId ?: 0,
                investorName = state.investorName.trim(),
                investorEmail = state.investorEmail.trim(),
                investorPhone = state.investorPhone.trim()
            )

            if (state.investorId != null) {
                repo.updateInvestor(investor)
            } else {
                repo.insertInvestor(investor)
            }

            _isSaved.value = true
            onSuccess()
        } catch (e: Exception) {
            onFail("Failed to save investor: ${e.localizedMessage}")
        }
    }

    private fun validate(state: InvestorFormState): Boolean {
        var valid = true

        if (state.investorName.isBlank()) {
            _formState.update { it.copy(nameError = "Name is required") }
            valid = false
        }

        if (state.investorPhone.isBlank()) {
            _formState.update { it.copy(phoneError = "Phone is required") }
            valid = false
        }

        return valid
    }
}
