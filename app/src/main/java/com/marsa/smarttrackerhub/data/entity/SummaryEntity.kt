package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Created by Muhammed Shafi on 25/06/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Entity(tableName = "summary")
data class SummaryEntity(
    @PrimaryKey
    val monthName: String,
    val cashBalance: Double,
    val accountBalance: Double,
    val crSaleBalance: Double
)