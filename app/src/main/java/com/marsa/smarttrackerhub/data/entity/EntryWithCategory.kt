package com.marsa.smarttrackerhub.data.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.marsa.smarttrackerhub.data.entity.EntryEntity


/**
 * Created by Muhammed Shafi on 20/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
data class EntryWithCategory(
    @Embedded val entry: EntryEntity,
    @ColumnInfo(name = "categoryName") val categoryName: String,
    @ColumnInfo(name = "vendorName") val vendorName: String
)