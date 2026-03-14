package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v7 → v8: shopRegion field added to shop_info.
 *
 * Changes:
 *  - shop_info: added `shopRegion` TEXT (UAE | KUWAIT | KSA), defaults to "UAE"
 *
 * Created by Muhammed Shafi on 13/03/2026.
 * Moro Hub
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `shop_info` ADD COLUMN `shopRegion` TEXT NOT NULL DEFAULT 'UAE'")
    }
}
