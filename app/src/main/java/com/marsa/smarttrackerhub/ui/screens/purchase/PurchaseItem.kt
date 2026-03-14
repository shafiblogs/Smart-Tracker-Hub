package com.marsa.smarttrackerhub.ui.screens.purchase

/**
 * Represents one category-level purchase row received from Firestore
 * (SmartTracker pushes this as `purchaseBreakdown` inside the monthly summary document).
 *
 * Created by Muhammed Shafi on 13/03/2026.
 * Moro Hub
 */
data class PurchaseItem(
    val categoryId: Int = 0,
    val categoryName: String = "",
    val totalAmount: Double = 0.0
)
