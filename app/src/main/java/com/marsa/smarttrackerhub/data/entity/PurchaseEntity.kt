package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing purchase breakdown data per month.
 * Synced from Firestore summary/{shopId}/months/{monthId}/purchaseBreakdown
 */
@Entity(
    tableName = "purchase_items",
    indices = [
        // UNIQUE on the business key so REPLACE de-dups per category — prevents the
        // append-duplicate bug regardless of which writer inserts.
        Index(
            name = "index_purchase_items_shopId_monthId_categoryId",
            value = ["shopId", "monthId", "categoryId"],
            unique = true
        )
    ]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val shopId: String,
    val monthId: String,
    val categoryId: Int,
    val categoryName: String,
    val totalAmount: Double,
    val lastUpdated: Long
)
