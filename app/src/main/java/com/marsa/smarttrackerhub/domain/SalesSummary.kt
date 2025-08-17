package com.marsa.smarttrackerhub.domain


/**
 * Created by Muhammed Shafi on 31/05/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
data class SalesSummary(
    val dateKey:String,
    val date: String,

    val totalSale: Double,
    val cashIn: Double,

    val cashSale: Double,
    val cardSale: Double,
    val creditSale: Double,
    val cashPayment: Double,
    val cardPayment: Double
)