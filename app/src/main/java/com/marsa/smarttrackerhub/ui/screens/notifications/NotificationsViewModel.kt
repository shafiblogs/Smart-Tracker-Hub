package com.marsa.smarttrackerhub.ui.screens.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Created by Muhammed Shafi on 16/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class NotificationsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private lateinit var repository: NotificationRepository

    fun initDatabase(context: Context) {
        val db = AppDatabase.getDatabase(context)
        repository = NotificationRepository(db.shopDao(), db.employeeDao())
        loadNotifications()
    }

    private fun loadNotifications() = viewModelScope.launch {
        repository.getNotifications().collect { notifications ->
            _uiState.value = NotificationsUiState(
                notifications = notifications,
                isLoading = false
            )
        }
    }

    fun refreshNotifications() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadNotifications()
    }
}