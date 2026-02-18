package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Per-investor line in a year-end settlement.
 * Captures who overpaid / underpaid and what was settled.
 *
 * balanceAmount = actualPaidAmount - fairShareAmount
 *   +ve → investor overpaid → others owe this investor
 *   -ve → investor underpaid → this investor owes others
 *
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Entity(
    tableName = "settlement_entry",
    foreignKeys = [
        ForeignKey(
            entity = YearEndSettlement::class,
            parentColumns = ["id"],
            childColumns = ["settlementId"],
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
        Index(value = ["settlementId"]),
        Index(value = ["investorId"])
    ]
)
data class SettlementEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val settlementId: Int,
    val investorId: Int,
    val fairShareAmount: Double,            // sharePercentage/100 × totalInvested
    val actualPaidAmount: Double,           // SUM of all their transactions in the period
    val balanceAmount: Double,              // actualPaid - fairShare
    val settlementPaidAmount: Double = 0.0, // Amount actually transferred to settle
    val settlementPaidDate: Long? = null
)
