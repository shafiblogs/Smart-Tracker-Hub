package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Created by Muhammed Shafi on 18/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Entity(tableName = "shop_info")
data class ShopInfo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopName: String,
    val shopAddress: String,
    val shopCode: String,
    val shopStatus: String,
    val shopType: String
)