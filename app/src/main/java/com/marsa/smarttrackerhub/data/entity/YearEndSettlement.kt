package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One record per settlement event for a shop.
 *
 * [settlementDate] is the user-chosen date marking the end of the period.
 * The period starts right after the previous settlement's [settlementDate]
 * (or from the beginning if this is the first settlement).
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
    val settlementDate: Long,               // User-chosen date marking end of the period
    val periodStartDate: Long,              // Start of the period (0 = all time for first settlement)
    val totalInvested: Double,              // Sum of all transactions in this period only
    val note: String = "",
    val isCarriedForward: Boolean = true    // Balances roll into next period
)
