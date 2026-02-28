package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v2 → v3: Shop status field added.
 *
 * Changes:
 *  - shop_info: added `shopStatus` TEXT column (default 'Initial')
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `shop_info` ADD COLUMN `shopStatus` TEXT NOT NULL DEFAULT 'Initial'"
        )
    }
}
