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
    val employeeId: String = "",   // Business-level identifier — used as Firebase document ID
    val employeeName: String,
    val employeePhone: String,
    val employeeRole: String,
    val salary: Double,
    val allowance: Double = 0.0,
    val associatedShopId: Int,
    val associatedShopFirebaseId: String = "", // Denormalized — Firestore path: /shops/{associatedShopFirebaseId}
    val visaExpiryDate: Long,
    val isActive: Boolean = true, // true = active, false = terminated
    val terminationDate: Long? = null, // Optional: track when they left
    val isSynced: Boolean = false, // False until pushed to Firestore
    val updatedAt: Long = 0L       // Last local-edit time; drives newest-wins on pull
)