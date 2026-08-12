package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v6 → v7 — consolidated migration adding all schema changes from v6 through v9:
 *
 * v6 → v7 changes: add `withdrawal` and `provision` to `account_summary`. These are published by
 * the AccountsTracker app in the region summary doc and surfaced on the Home account card.
 *
 * v7 → v8 changes: add `vatPurchase` and `creditPurchase` to `summary`. These are published by the
 * SmartTracker app in the sales summary doc and shown on the Sales list + detail screens.
 *
 * v8 → v9 changes: add `accountBalance` and `openingAccountBalance` to `account_summary`. These carry
 * the bank/account balance (distinct from cash balance) shown on the Account detail statement
 * card's opening→closing rows.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // v6 → v7: account_summary fields
        database.execSQL("ALTER TABLE `account_summary` ADD COLUMN `withdrawal` REAL NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE `account_summary` ADD COLUMN `provision` REAL NOT NULL DEFAULT 0")

        // v7 → v8: summary fields
        database.execSQL("ALTER TABLE `summary` ADD COLUMN `vatPurchase` REAL NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE `summary` ADD COLUMN `creditPurchase` REAL NOT NULL DEFAULT 0")

        // v8 → v9: account_summary fields
        database.execSQL("ALTER TABLE `account_summary` ADD COLUMN `accountBalance` REAL NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE `account_summary` ADD COLUMN `openingAccountBalance` REAL NOT NULL DEFAULT 0")
    }
}
