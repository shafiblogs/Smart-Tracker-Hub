package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Created by Muhammed Shafi on 28/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

@Entity(tableName = "vendors")
data class Vendor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val contact: String,
    val notes: String? = null,
    val categoryId: Int // FK to `categories.id`
)