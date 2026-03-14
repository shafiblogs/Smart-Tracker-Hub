package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v6 → v7: isSynced flag added to all Firebase-push-able tables.
 *          Missing Firebase path fields added to remaining tables.
 *
 * Changes:
 *  - shop_info:              added `isSynced` INTEGER (0 = not synced)
 *  - investor_info:          added `isSynced` INTEGER
 *  - employee_info:          added `associatedShopFirebaseId` TEXT (denormalized shop Firebase ID)
 *                            added `isSynced` INTEGER
 *  - shop_investor:          added `isSynced` INTEGER
 *  - investment_transaction: added `isSynced` INTEGER
 *  - year_end_settlement:    added `shopFirebaseId` TEXT (denormalized for Firestore path)
 *                            added `isSynced` INTEGER
 *  - settlement_entry:       added `settlementFirebaseId` TEXT (parent settlement doc ID)
 *                            added `shopFirebaseId` TEXT (denormalized for Firestore path)
 *                            added `isSynced` INTEGER
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // shop_info — sync flag
        db.execSQL("ALTER TABLE `shop_info` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")

        // investor_info — sync flag
        db.execSQL("ALTER TABLE `investor_info` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")

        // employee_info — denormalized shop Firebase ID + sync flag
        db.execSQL("ALTER TABLE `employee_info` ADD COLUMN `associatedShopFirebaseId` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `employee_info` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")

        // shop_investor — sync flag
        db.execSQL("ALTER TABLE `shop_investor` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")

        // investment_transaction — sync flag
        db.execSQL("ALTER TABLE `investment_transaction` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")

        // year_end_settlement — denormalized shop Firebase ID + sync flag
        db.execSQL("ALTER TABLE `year_end_settlement` ADD COLUMN `shopFirebaseId` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `year_end_settlement` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")

        // settlement_entry — parent settlement doc ID + denormalized shop Firebase ID + sync flag
        db.execSQL("ALTER TABLE `settlement_entry` ADD COLUMN `settlementFirebaseId` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `settlement_entry` ADD COLUMN `shopFirebaseId` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `settlement_entry` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")
    }
}
