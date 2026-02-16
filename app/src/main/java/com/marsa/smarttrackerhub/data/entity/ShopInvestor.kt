package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Junction table linking shops with investors and their share details
 * Created by Muhammed Shafi on 16/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Entity(
    tableName = "shop_investor",
    foreignKeys = [
        ForeignKey(
            entity = ShopInfo::class,
            parentColumns = ["id"],
            childColumns = ["shopId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InvestorInfo::class,
            parentColumns = ["id"],
            childColumns = ["investorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["shopId"]),
        Index(value = ["investorId"])
    ]
)
data class ShopInvestor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopId: Int,
    val investorId: Int,
    val sharePercentage: Double, // e.g., 40.5
    val investmentAmount: Double, // Actual AED invested
    val investmentDate: Long
)