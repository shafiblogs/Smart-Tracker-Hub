package com.marsa.smarttrackerhub.ui.screens.employees

/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
data class EmployeeFormState(
    val employeeId: String = "",        // Business-level identifier — used as Firebase document ID
    val employeeName: String = "",
    val employeePhone: String = "",
    val employeeRole: EmployeeRole? = null,
    val salary: String = "",
    val allowance: String = "",
    val associatedShopId: Int? = null,
    val visaExpiryDate: Long? = null
)