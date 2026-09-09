package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v7 → v8: incremental-sync bookkeeping.
 *
 * Adds the sync_markers table that records, per sync target, the incremental cursor
 * (max remote `updatedAt` seen) and the last full-collection pull time. Lets each pull
 * skip unchanged documents and reconcile fully on a schedule instead of every screen
 * refresh re-reading whole collections — same shape as AccountsTracker's SyncPolicy.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
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
    }
}
