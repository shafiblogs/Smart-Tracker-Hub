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
            val twoMonthsInMillis = TimeUnit.DAYS.toMillis(60)

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

                // Check Zakath due date
                val zakathDueDate = calculateNextZakathDueDate(shop.stockTakenDate)
                val daysUntilZakathDue = TimeUnit.MILLISECONDS.toDays(zakathDueDate - currentTime)
                val zakathAmount = shop.stockValue * 0.025

                when {
                    daysUntilZakathDue <= 0 -> {
                        // Zakath is due
                        notifications.add(
                            NotificationItem(
                                id = "zakath_due_${shop.id}",
                                type = NotificationType.ZAKATH_DUE,
                                title = "Zakath Payment Due",
                                message = "${shop.shopName} - Time to update stock and calculate Zakath",
                                expiryDate = zakathDueDate,
                                entityId = shop.id,
                                entityName = shop.shopName,
                                priority = NotificationPriority.HIGH,
                                additionalInfo = "Zakath Amount: AED ${String.format("%.2f", zakathAmount)}"
                            )
                        )
                    }
                    daysUntilZakathDue <= 30 -> {
                        // Zakath approaching (within 1 month)
                        notifications.add(
                            NotificationItem(
                                id = "zakath_approaching_${shop.id}",
                                type = NotificationType.ZAKATH_APPROACHING,
                                title = "Zakath Due Soon",
                                message = "${shop.shopName} - Zakath due in $daysUntilZakathDue days",
                                expiryDate = zakathDueDate,
                                entityId = shop.id,
                                entityName = shop.shopName,
                                priority = NotificationPriority.MEDIUM,
                                additionalInfo = "Current Zakath: AED ${String.format("%.2f", zakathAmount)}"
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
     * Calculate the next Zakath due date (one lunar year from stock taken date)
     * Zakath is calculated based on Islamic lunar year (354-355 days)
     * For simplicity, using 354 days as standard lunar year
     */
    private fun calculateNextZakathDueDate(stockTakenDate: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = stockTakenDate

        // Add one lunar year (354 days) to the stock taken date
        calendar.add(Calendar.DAY_OF_YEAR, 354)

        return calendar.timeInMillis
    }
}