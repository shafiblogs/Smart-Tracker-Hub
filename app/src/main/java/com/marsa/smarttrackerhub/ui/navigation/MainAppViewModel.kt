package com.marsa.smarttrackerhub.ui.navigation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.domain.AccessCode
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
    private val _userAccessCode = MutableStateFlow<AccessCode>(AccessCode.GUEST)
    val userAccessCode: StateFlow<AccessCode> = _userAccessCode.asStateFlow()

    private val _isAccountActive = MutableStateFlow(false)
    val isAccountActive: StateFlow<Boolean> = _isAccountActive.asStateFlow()

    fun loadUserAccount() {
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication())
            val account = db.userAccountDao().getFirstAccount()

            if (account != null) {
                _isAccountActive.value = true
                _userAccessCode.value = AccessCode.fromRole(account.userRole)
            } else {
                _isAccountActive.value = false
                _userAccessCode.value = AccessCode.GUEST
            }
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