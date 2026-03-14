package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v3 → v4: totalInvested field added to shop_info.
 *
 * Changes:
 *  - shop_info: added `totalInvested` REAL column (default 0.0)
 *    Cached sum of all investment_transaction amounts for the shop.
 *    Updated whenever a transaction is added, edited, or deleted.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `shop_info` ADD COLUMN `totalInvested` REAL NOT NULL DEFAULT 0.0"
        )
    }
}
