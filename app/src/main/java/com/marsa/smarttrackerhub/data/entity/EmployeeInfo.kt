package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Entity(tableName = "employee_info")
data class EmployeeInfo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeName: String,
    val employeePhone: String,
    val employeeRole: String,
    val salary: Double,
    val allowance: Double = 0.0,
    val associatedShopId: Int,
    val visaExpiryDate: Long,
    val isActive: Boolean = true, // New field - true = active, false = terminated
    val terminationDate: Long? = null // Optional: track when they left
)