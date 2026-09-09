package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v7 → v8 — production is currently on v7 (quality/master have never seen an intermediate
 * version), so this is a single consolidated migration rather than separate steps, matching
 * the same squashing pattern MIGRATION_6_7 already used:
 *
 *  1. sync_markers — incremental-sync bookkeeping. Records, per sync target, the incremental
 *     cursor (max remote `updatedAt` seen) and the last full-collection pull time. Lets each
 *     pull skip unchanged documents and reconcile fully on a schedule instead of every screen
 *     refresh re-reading whole collections — same shape as AccountsTracker's SyncPolicy.
 *
 *  2. cached_log_entries — local cache for the Logs screen. Previously LogsViewModel had no
 *     Room cache at all — every selection change hit Firestore directly. Lets a repeat
 *     selection within SyncPolicy's TTL window be served from Room instead, without showing
 *     a blank screen while doing so.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1 — sync_markers
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sync_markers` (
                `key` TEXT NOT NULL PRIMARY KEY,
                `lastSeenRemoteMs` INTEGER NOT NULL DEFAULT 0,
                `lastPullMs` INTEGER NOT NULL DEFAULT 0,
                `lastFullPullMs` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        // 2 — cached_log_entries
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
