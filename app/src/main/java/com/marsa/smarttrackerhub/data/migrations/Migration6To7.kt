package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v6 → v7 — add `withdrawal` and `provision` to `account_summary`. These are published by
 * the AccountsTracker app in the region summary doc and surfaced on the Home account card.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `account_summary` ADD COLUMN `withdrawal` REAL NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE `account_summary` ADD COLUMN `provision` REAL NOT NULL DEFAULT 0")
    }
}
