package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Records every actual money contribution by an investor to a shop.
 * Investors pay in phases (Phase 1, Phase 2, …) and each payment is a row here.
 *
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Entity(
    tableName = "investment_transaction",
    foreignKeys = [
        ForeignKey(
            entity = ShopInvestor::class,
            parentColumns = ["id"],
            childColumns = ["shopInvestorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["shopInvestorId"])]
)
data class InvestmentTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopInvestorId: Int,            // FK → ShopInvestor.id
    val amount: Double,                  // AED paid in this transaction
    val transactionDate: Long,
    val phase: String,                   // "Phase 1", "Phase 2", "Initial Setup", etc.
    val note: String = ""
)
