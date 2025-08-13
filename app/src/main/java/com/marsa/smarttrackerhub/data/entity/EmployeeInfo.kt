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
    val employeeEmail: String,
    val employeePhone: String,
    val employeeRole: String,
    val salary: Double,
    val associatedShopId: Int // FK reference to ShopInfo.id
)