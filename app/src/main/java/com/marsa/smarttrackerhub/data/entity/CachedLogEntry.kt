package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local cache of one shop's/employee's shop-open-close and login/logout events for one month,
 * keyed by shopFirebaseId + monthKey ("yyyy-MM") — mirrors what LogsViewModel previously
 * fetched fresh from Firestore on every selection change. Lets a TTL-skip in LogsViewModel
 * still show the right data instead of a blank screen.
 */
@Entity(
    tableName = "cached_log_entries",
    indices = [Index(value = ["shopFirebaseId", "monthKey"])]
)
data class CachedLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopFirebaseId: String,
    val monthKey: String,
    val date: String,
    val eventType: String,
    val timestamp: Long,
    val employeeId: String,
    val employeeName: String
)
