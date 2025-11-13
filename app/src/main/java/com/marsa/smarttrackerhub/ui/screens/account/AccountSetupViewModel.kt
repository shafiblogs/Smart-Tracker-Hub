package com.marsa.smarttrackerhub.ui.screens.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.UserAccount
import com.marsa.smarttrackerhub.data.repository.UserAccountRepository
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.helper.AuthHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * Created by Muhammed Shafi on 18/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

class AccountSetupViewModel : ViewModel() {
    private val _formState = MutableStateFlow(AccountFormState())
    val formState = _formState.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var editingAccountId: Int? = null

    val isFormValid: StateFlow<Boolean> = formState
        .map {
            it.accessCode.isNotBlank() &&
                    it.userName.isNotBlank() &&
                    it.password.length >= 4 &&
                    it.password == it.confirmPassword
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun loadExistingAccount(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val account = db.userAccountDao().getFirstAccount()

        if (account != null) {
            editingAccountId = account.id

            val accessCode = AccessCode.fromRole(account.userRole)

            _formState.value = AccountFormState(
                accessCode = accessCode.code,
                userName = account.userName,
                password = account.password,
                confirmPassword = account.password
            )
            _isLoaded.value = true
        }
    }

    fun updateUserCode(accessCode: String) {
        _formState.update { it.copy(accessCode = accessCode) }
        _error.value = null
    }

    fun updateUserName(name: String) {
        _formState.update { it.copy(userName = name) }
        _error.value = null
    }

    fun updatePassword(pw: String) {
        _formState.update { it.copy(password = pw) }
        _error.value = null
    }

    fun updateConfirmPassword(pw: String) {
        _formState.update { it.copy(confirmPassword = pw) }
        _error.value = null
    }

    fun saveAccount(
        context: Context,
        onSuccess: () -> Unit = {},
        onFail: (String) -> Unit = {}
    ) = viewModelScope.launch {
        val state = _formState.value

        // Validate password and confirm password match
        if (state.password != state.confirmPassword) {
            onFail("Passwords do not match")
            return@launch
        }

        if (state.userName.length < 4) {
            onFail("Username must be at least 4 characters")
            return@launch
        }

        if (state.password.length < 4) {
            onFail("Password must be at least 4 characters")
            return@launch
        }

        try {
            val db = AppDatabase.getDatabase(context)
            val repo = UserAccountRepository(db.userAccountDao())
            // Get AccessCode - automatically defaults to GUEST for any invalid code
            val accessCode = AccessCode.fromCode(state.accessCode.trim())

            val account = UserAccount(
                id = editingAccountId ?: 0,
                userRole = accessCode.roleName,
                userName = state.userName,
                password = state.password
            )

            if (editingAccountId != null) {
                repo.updateAccount(account)
            } else {
                repo.insertAccount(account)
            }
            _isSaved.value = true

//            AuthHelper.updateFireStore(context, account, onSuccess = {
//                onSuccess()
//            }, onFail)

        } catch (e: Exception) {
            onFail("Failed to save account: ${e.localizedMessage}")
        }
    }
}