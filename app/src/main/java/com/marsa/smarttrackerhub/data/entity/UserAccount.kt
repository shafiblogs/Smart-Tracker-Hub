package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Created by Muhammed Shafi on 18/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountName: String,
    val accountType: String, // "personal" or "business"
    val address: String,
    val userName: String,
    val password: String
)