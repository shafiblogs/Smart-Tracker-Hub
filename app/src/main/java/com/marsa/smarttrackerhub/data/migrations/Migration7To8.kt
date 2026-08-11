package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v7 → v8 — add `vatPurchase` and `creditPurchase` to `summary`. These are published by the
 * SmartTracker app in the sales summary doc and shown on the Sales list + detail screens.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `summary` ADD COLUMN `vatPurchase` REAL NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE `summary` ADD COLUMN `creditPurchase` REAL NOT NULL DEFAULT 0")
    }
}
