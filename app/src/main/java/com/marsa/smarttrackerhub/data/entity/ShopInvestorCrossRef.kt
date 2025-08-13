package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

@Entity(primaryKeys = ["shopId", "investorId"])
data class ShopInvestorCrossRef(
    val shopId: Int,
    val investorId: Int,
    val investmentAmount: Double = 0.0 // Optional
)