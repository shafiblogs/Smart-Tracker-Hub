package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Entity(tableName = "investor_info")
data class InvestorInfo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val investorName: String,
    val investorEmail: String,
    val investorPhone: String,
    val investmentAmount: Double,
    val associatedShopId: Int // FK reference to ShopInfo.id
)