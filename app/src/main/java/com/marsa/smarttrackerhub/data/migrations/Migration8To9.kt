package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v8 → v9 — add `accountBalance` and `openingAccountBalance` to `account_summary`. These carry
 * the bank/account balance (distinct from cash balance) shown on the Account detail statement
 * card's opening→closing rows.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `account_summary` ADD COLUMN `accountBalance` REAL NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE `account_summary` ADD COLUMN `openingAccountBalance` REAL NOT NULL DEFAULT 0")
    }
}
