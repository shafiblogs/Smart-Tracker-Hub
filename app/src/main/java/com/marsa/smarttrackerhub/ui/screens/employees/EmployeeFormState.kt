package com.marsa.smarttrackerhub.ui.screens.employees

/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
data class EmployeeFormState(
    val employeeName: String = "",
    val employeePhone: String = "",
    val employeeRole: EmployeeRole? = null, // Changed to enum
    val salary: String = "", // Use String for user input
    val allowance: String = "", // New field
    val associatedShopId: Int? = null,
    val visaExpiryDate: Long? = null // New field
)