package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v6 → v7 — Add `updatedAt` to the 7 Hub-synced tables to enable newest-wins edit
 * propagation across devices. Default 0 = "baseline/unknown": existing rows and
 * pre-timestamp Firestore docs compare equal (0 == 0) so neither overwrites the other;
 * the first real edit stamps `now()` and wins.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        listOf(
            "shop_info",
            "employee_info",
            "investor_info",
            "shop_investor",
            "investment_transaction",
            "year_end_settlement",
            "settlement_entry"
        ).forEach { table ->
            database.execSQL("ALTER TABLE `$table` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        }
    }
}
