package com.marsa.smarttrackerhub.ui.screens.investers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.entity.InvestorInfo
import com.marsa.smarttrackerhub.data.repository.InvestorRepository
import kotlinx.coroutines.launch


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class InvestorAddViewModel(
    private val repository: InvestorRepository
) : ViewModel() {

    var formState by mutableStateOf(InvestorFormState())
        private set

    fun updateName(value: String) {
        formState = formState.copy(investorName = value)
    }

    fun updateEmail(value: String) {
        formState = formState.copy(investorEmail = value)
    }

    fun updatePhone(value: String) {
        formState = formState.copy(investorPhone = value)
    }

    fun saveInvestor() {
        viewModelScope.launch {
            repository.insertInvestor(
                InvestorInfo(
                    investorName = formState.investorName,
                    investorEmail = formState.investorEmail,
                    investorPhone = formState.investorPhone
                )
            )
        }
    }
}