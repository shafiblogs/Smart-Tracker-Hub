package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v8 → v9: Fix employees that were silently skipped during Firebase sync.
 *
 * Pre-v5 employees had `employeeId = ''` (migration default). The sync code was
 * marking them as isSynced=1 without actually pushing to Firestore. This migration
 * resets isSynced=0 for all such rows so syncAll() will re-process them — this time
 * auto-assigning a UUID and properly pushing each one to Firestore.
 *
 * Created by Muhammed Shafi on 16/03/2026.
 * Moro Hub
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Reset employees with blank employeeId so they get re-synced with a UUID
        db.execSQL("UPDATE `employee_info` SET `isSynced` = 0 WHERE `employeeId` = ''")
    }
}
