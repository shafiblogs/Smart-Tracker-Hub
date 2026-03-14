package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v4 → v5: employeeId field added to employee_info.
 *
 * Changes:
 *  - employee_info: added `employeeId` TEXT column (default '')
 *    Business-level identifier used as Firebase document ID.
 *    Mirrors the role of `shopId` in shop_info.
 *    Set when creating or editing an employee record.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `employee_info` ADD COLUMN `employeeId` TEXT NOT NULL DEFAULT ''"
        )
    }
}
