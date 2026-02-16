package com.marsa.smarttrackerhub.ui.screens.notifications

/**
 * Created by Muhammed Shafi on 16/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

enum class NotificationType {
    SHOP_LICENSE_EXPIRED,
    SHOP_LICENSE_NEAR_EXPIRY,
    EMPLOYEE_VISA_EXPIRED,
    EMPLOYEE_VISA_NEAR_EXPIRY,
    ZAKATH_DUE,
    ZAKATH_APPROACHING
}

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val expiryDate: Long,
    val entityId: Int,
    val entityName: String,
    val priority: NotificationPriority,
    val additionalInfo: String? = null
)

enum class NotificationPriority {
    HIGH,
    MEDIUM
}

data class NotificationsUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = false
)