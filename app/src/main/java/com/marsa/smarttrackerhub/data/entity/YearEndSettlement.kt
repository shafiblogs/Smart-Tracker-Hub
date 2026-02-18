package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One record per shop per year when the annual settlement is confirmed.
 * Captures the total invested in that period and when it was settled.
 *
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Entity(
    tableName = "year_end_settlement",
    foreignKeys = [
        ForeignKey(
            entity = ShopInfo::class,
            parentColumns = ["id"],
            childColumns = ["shopId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["shopId"])]
)
data class YearEndSettlement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopId: Int,
    val year: Int,                          // e.g. 2025
    val totalInvested: Double,              // Sum of all transactions in this period
    val settlementDate: Long,
    val note: String = "",
    val isCarriedForward: Boolean = true    // Balances roll into next year
)
