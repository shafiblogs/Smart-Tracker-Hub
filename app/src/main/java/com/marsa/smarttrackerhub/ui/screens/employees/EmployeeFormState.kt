package com.marsa.smarttrackerhub.ui.screens.employees


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
data class EmployeeFormState(
    val employeeName: String = "",
    val employeeEmail: String = "",
    val employeePhone: String = "",
    val employeeRole: String = "",
    val salary: String = "", // Use String for user input, convert on save
    val associatedShopId: Int? = null
)