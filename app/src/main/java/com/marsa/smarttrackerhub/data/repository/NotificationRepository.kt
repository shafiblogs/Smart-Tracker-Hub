package com.marsa.smarttrackerhub.data.repository

import com.marsa.smarttrackerhub.data.dao.EmployeeInfoDao
import com.marsa.smarttrackerhub.data.dao.ShopDao
import com.marsa.smarttrackerhub.ui.screens.notifications.NotificationItem
import com.marsa.smarttrackerhub.ui.screens.notifications.NotificationPriority
import com.marsa.smarttrackerhub.ui.screens.notifications.NotificationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Created by Muhammed Shafi on 16/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class NotificationRepository(
    private val shopDao: ShopDao,
    private val employeeDao: EmployeeInfoDao
) {

    fun getNotifications(): Flow<List<NotificationItem>> {
        return combine(
            shopDao.getAllShops(),
            employeeDao.getActiveEmployees()
        ) { shops, employees ->
            val notifications = mutableListOf<NotificationItem>()
            val currentTime = System.currentTimeMillis()

            // Check shop licenses
            shops.forEach { shop ->
                val daysUntilExpiry = TimeUnit.MILLISECONDS.toDays(shop.licenseExpiryDate - currentTime)

                when {
                    daysUntilExpiry < 0 -> {
                        // Expired
                        notifications.add(
                            NotificationItem(
                                id = "shop_license_${shop.id}",
                                type = NotificationType.SHOP_LICENSE_EXPIRED,
                                title = "Shop License Expired",
                                message = "${shop.shopName} - License has expired",
                                expiryDate = shop.licenseExpiryDate,
                                entityId = shop.id,
                                entityName = shop.shopName,
                                priority = NotificationPriority.HIGH
                            )
                        )
                    }
                    daysUntilExpiry <= 60 -> {
                        // Near expiry (within 2 months)
                        notifications.add(
                            NotificationItem(
                                id = "shop_license_${shop.id}",
                                type = NotificationType.SHOP_LICENSE_NEAR_EXPIRY,
                                title = "Shop License Expiring Soon",
                                message = "${shop.shopName} - License expires in $daysUntilExpiry days",
                                expiryDate = shop.licenseExpiryDate,
                                entityId = shop.id,
                                entityName = shop.shopName,
                                priority = NotificationPriority.MEDIUM
                            )
                        )
                    }
                }

                // Check Zakath Payment Status - if pending, show notification
                if (shop.zakathStatus == "Pending") {
                    val zakathAmount = shop.stockValue * 0.025
                    notifications.add(
                        NotificationItem(
                            id = "zakath_pending_${shop.id}",
                            type = NotificationType.ZAKATH_PAYMENT_PENDING,
                            title = "Zakath Payment Pending",
                            message = "${shop.shopName} - Zakath payment is pending",
                            expiryDate = shop.stockTakenDate, // Use stock taken date as reference
                            entityId = shop.id,
                            entityName = shop.shopName,
                            priority = NotificationPriority.HIGH,
                            additionalInfo = "Pending Amount: AED ${String.format("%.2f", zakathAmount)}"
                        )
                    )
                }

                // Check Zakath stock taking due date
                val nextStockDueDate = calculateNextZakathStockDate(shop.stockTakenDate)
                val daysUntilStockDue = TimeUnit.MILLISECONDS.toDays(nextStockDueDate - currentTime)

                when {
                    daysUntilStockDue <= 0 -> {
                        // Stock taking is due
                        notifications.add(
                            NotificationItem(
                                id = "zakath_stock_due_${shop.id}",
                                type = NotificationType.ZAKATH_STOCK_DUE,
                                title = "Stock Taking Due for Zakath",
                                message = "${shop.shopName} - Time to update stock for Zakath calculation",
                                expiryDate = nextStockDueDate,
                                entityId = shop.id,
                                entityName = shop.shopName,
                                priority = NotificationPriority.HIGH,
                                additionalInfo = "Update stock value and calculate Zakath"
                            )
                        )
                    }
                    daysUntilStockDue <= 30 -> {
                        // Stock taking approaching (within 1 month)
                        notifications.add(
                            NotificationItem(
                                id = "zakath_stock_approaching_${shop.id}",
                                type = NotificationType.ZAKATH_STOCK_APPROACHING,
                                title = "Stock Taking Approaching",
                                message = "${shop.shopName} - Stock taking due in $daysUntilStockDue days",
                                expiryDate = nextStockDueDate,
                                entityId = shop.id,
                                entityName = shop.shopName,
                                priority = NotificationPriority.MEDIUM,
                                additionalInfo = "Prepare for annual Zakath stock count"
                            )
                        )
                    }
                }
            }

            // Check employee visas
            employees.forEach { employee ->
                val daysUntilExpiry = TimeUnit.MILLISECONDS.toDays(employee.visaExpiryDate - currentTime)

                when {
                    daysUntilExpiry < 0 -> {
                        // Expired
                        notifications.add(
                            NotificationItem(
                                id = "employee_visa_${employee.id}",
                                type = NotificationType.EMPLOYEE_VISA_EXPIRED,
                                title = "Employee Visa Expired",
                                message = "${employee.employeeName} - Visa has expired",
                                expiryDate = employee.visaExpiryDate,
                                entityId = employee.id,
                                entityName = employee.employeeName,
                                priority = NotificationPriority.HIGH
                            )
                        )
                    }
                    daysUntilExpiry <= 60 -> {
                        // Near expiry (within 2 months)
                        notifications.add(
                            NotificationItem(
                                id = "employee_visa_${employee.id}",
                                type = NotificationType.EMPLOYEE_VISA_NEAR_EXPIRY,
                                title = "Employee Visa Expiring Soon",
                                message = "${employee.employeeName} - Visa expires in $daysUntilExpiry days",
                                expiryDate = employee.visaExpiryDate,
                                entityId = employee.id,
                                entityName = employee.employeeName,
                                priority = NotificationPriority.MEDIUM
                            )
                        )
                    }
                }
            }

            // Sort by priority (HIGH first) and then by expiry date
            notifications.sortedWith(
                compareBy<NotificationItem> { it.priority }
                    .thenBy { it.expiryDate }
            )
        }
    }

    /**
     * Calculate the next Zakath stock taking date (one lunar year from last stock taken date)
     * Zakath is calculated based on Islamic lunar year (354-355 days)
     * Using 354 days as standard lunar year
     */
    private fun calculateNextZakathStockDate(lastStockTakenDate: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastStockTakenDate

        // Add one lunar year (354 days) to the last stock taken date
        calendar.add(Calendar.DAY_OF_YEAR, 354)

        return calendar.timeInMillis
    }
}