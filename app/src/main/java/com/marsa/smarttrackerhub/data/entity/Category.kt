package com.marsa.smarttrackerhub.data.entity


/**
 * Created by Muhammed Shafi on 20/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String? = null,
    val screenType: String
)
