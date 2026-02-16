package com.marsa.smarttrackerhub.data.repository

import com.marsa.smarttrackerhub.data.dao.EmployeeInfoDao
import com.marsa.smarttrackerhub.data.dao.ShopDao
import com.marsa.smarttrackerhub.ui.screens.notifications.NotificationItem
import com.marsa.smarttrackerhub.ui.screens.notifications.NotificationPriority
import com.marsa.smarttrackerhub.ui.screens.notifications.NotificationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
}