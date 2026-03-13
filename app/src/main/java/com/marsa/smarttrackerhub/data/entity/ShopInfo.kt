package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_info")
data class ShopInfo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopName: String,
    val shopAddress: String,
    val shopId: String,
    val zakathStatus: String, // Changed from shopStatus
    val shopType: String,
    val licenseExpiryDate: Long,
    val shopOpeningDate: Long, // New field - opening date
    val stockValue: Double = 0.0, // New field - current stock value
    val stockTakenDate: Long = 0L, // New field - when stock was last counted
    val shopStatus: String = "Initial", // Running | Initial | Closed
    val totalInvested: Double = 0.0,   // Cached total from investment_transaction
    val isSynced: Boolean = false,      // False until pushed to Firestore
    val shopRegion: String = "UAE"      // UAE | KUWAIT | KSA — used for access-code filtering
)