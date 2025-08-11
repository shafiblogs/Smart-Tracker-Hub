package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate


/**
 * Created by Muhammed Shafi on 22/06/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    //val type: EntryType,
    val amount: String,
    val date: LocalDate,
    val categoryId: Int,
    val paymentType: String,
    val vendorId: Int
)