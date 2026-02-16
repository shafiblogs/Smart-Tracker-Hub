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
    ZAKATH_STOCK_DUE,              // New: Time to take stock for Zakath calculation
    ZAKATH_STOCK_APPROACHING,      // New: Stock taking date approaching
    ZAKATH_PAYMENT_PENDING         // New: Zakath calculated but not paid
}

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val expiryDate: Long,
    val entityId: Int, // Shop ID or Employee ID
    val entityName: String,
    val priority: NotificationPriority,
    val additionalInfo: String? = null // For showing stock value, zakath amount etc.
)

enum class NotificationPriority {
    HIGH,    // Expired / Due / Pending Payment
    MEDIUM   // Near expiry / Approaching
}

data class NotificationsUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = false
)