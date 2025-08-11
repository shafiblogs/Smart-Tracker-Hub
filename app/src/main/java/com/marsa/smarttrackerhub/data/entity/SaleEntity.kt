package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Created by Muhammed Shafi on 30/06/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val date: String,
    val cashSale: Double,
    val cardSale: Double,
    val creditSale: Double,
    val cashPayment: Double,
    val cardPayment: Double
)
