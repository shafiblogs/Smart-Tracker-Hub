package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v8 → v9: local cache for the Logs screen.
 *
 * Previously LogsViewModel had no Room cache at all — every selection change hit Firestore
 * directly. Adds cached_log_entries so a repeat selection within SyncPolicy's TTL window can
 * be served from Room instead, without showing a blank screen while doing so.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `cached_log_entries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `shopFirebaseId` TEXT NOT NULL,
                `monthKey` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `eventType` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `employeeId` TEXT NOT NULL,
                `employeeName` TEXT NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cached_log_entries_shopFirebaseId_monthKey` " +
                "ON `cached_log_entries` (`shopFirebaseId`, `monthKey`)"
        )
    }
}
