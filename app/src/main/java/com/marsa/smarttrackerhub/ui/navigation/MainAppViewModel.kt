package com.marsa.smarttrackerhub.ui.navigation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


/**
 * Created by Muhammed Shafi on 20/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

class MainAppViewModel(application: Application) : AndroidViewModel(application) {
    private val _isAdminUser = MutableStateFlow(false)
    val isAdminUser: StateFlow<Boolean> = _isAdminUser.asStateFlow()

    private val _isAccountActive = MutableStateFlow(false)
    val isAccountActive: StateFlow<Boolean> = _isAccountActive.asStateFlow()

    private val _isGuestUser = MutableStateFlow(true)
    val isGuestUser: StateFlow<Boolean> = _isGuestUser.asStateFlow()

    fun loadUserAccount() {
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication())
            val account = db.userAccountDao().getFirstAccount()
            _isAccountActive.value = account != null
            _isAdminUser.value = account?.userRole.equals("admin", ignoreCase = true)
            _isGuestUser.value = account?.userRole.equals("guest", ignoreCase = true)
        }
    }

//    fun updateFireStore() = viewModelScope.launch {
//        val db = AppDatabase.getDatabase(getApplication())
//        val account = db.userAccountDao().getFirstAccount()
//        if (account != null && checkValidShop(account)) {
//            //AuthHelper.updateFireStore(getApplication(), account)
//        }
//    }
}