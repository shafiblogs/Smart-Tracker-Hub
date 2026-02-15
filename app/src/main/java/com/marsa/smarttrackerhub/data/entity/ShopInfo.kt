package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_info")
data class ShopInfo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopName: String,
    val shopAddress: String,
    val shopId: String, // Changed from shopCode
    val shopStatus: String,
    val shopType: String,
    val licenseExpiryDate: Long // Added: timestamp in milliseconds
)