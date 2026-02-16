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
    EMPLOYEE_VISA_NEAR_EXPIRY
}

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val expiryDate: Long,
    val entityId: Int, // Shop ID or Employee ID
    val entityName: String,
    val priority: NotificationPriority
)

enum class NotificationPriority {
    HIGH,    // Expired
    MEDIUM   // Near expiry
}

data class NotificationsUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = false
)