package com.marsa.smarttrackerhub.ui.screens.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.repository.UserAccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * Created by Muhammed Shafi on 03/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class LoginViewModel : ViewModel() {
    private val _formData = MutableStateFlow(LoginModel())
    val formData: StateFlow<LoginModel> = _formData.asStateFlow()

    private val _isLoggedInSuccess = MutableStateFlow(false)
    val isLoggedInSuccess: StateFlow<Boolean> = _isLoggedInSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun checkAccountAvailability(context: Context) = viewModelScope.launch {
        val db = AppDatabase.getDatabase(context)
        val repo = UserAccountRepository(db.userAccountDao())
        val account = repo.getFirstAccount()
        _formData.update { it.copy(userName = account?.userName ?: "") }
    }

    val isFormValid: StateFlow<Boolean> = _formData
        .map { it.userName.isNotBlank() && it.password.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun updateUserName(userName: String) {
        _formData.update { it.copy(userName = userName) }
        _errorMessage.value = null
    }

    fun updatePassword(password: String) {
        _formData.update { it.copy(password = password) }
        _errorMessage.value = null
    }

    fun checkLoggedIn(context: Context) = viewModelScope.launch {
        val username = _formData.value.userName.trim()
        val password = _formData.value.password

        // Hardcoded admin check
        if (username == "admin" && password == "424356") {
            _isLoggedInSuccess.value = true
            _errorMessage.value = null
            return@launch
        }

        // DB-based authentication
        val db = AppDatabase.getDatabase(context)
        val repo = UserAccountRepository(db.userAccountDao())
        val account = repo.authenticate(username, password)

        _isLoggedInSuccess.value = account != null
        _errorMessage.value = if (account != null) null else "Invalid username or password"
    }

}
