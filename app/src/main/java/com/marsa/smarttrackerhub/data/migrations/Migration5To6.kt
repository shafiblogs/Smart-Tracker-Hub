package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v5 → v6: Firebase document ID fields added to all investment-related tables.
 *
 * Changes:
 *  - investor_info:          added `investorId` TEXT (Firebase doc ID, entered by admin)
 *  - shop_investor:          added `shopInvestorFirebaseId` TEXT (auto: "{shop.shopId}_{investor.investorId}")
 *  - investment_transaction: added `transactionFirebaseId` TEXT (UUID, auto on insert)
 *                            added `shopFirebaseId` TEXT (denormalized for Firestore path)
 *                            added `investorFirebaseId` TEXT (denormalized for Firestore path)
 *  - year_end_settlement:    added `settlementFirebaseId` TEXT (UUID, auto on confirm)
 *  - settlement_entry:       added `entryFirebaseId` TEXT (UUID, auto on confirm)
 *                            added `investorFirebaseId` TEXT (denormalized for Firestore path)
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // investor_info — business-level Firebase document ID
        db.execSQL("ALTER TABLE `investor_info` ADD COLUMN `investorId` TEXT NOT NULL DEFAULT ''")

        // shop_investor — composite Firebase doc ID: "{shop.shopId}_{investor.investorId}"
        db.execSQL("ALTER TABLE `shop_investor` ADD COLUMN `shopInvestorFirebaseId` TEXT NOT NULL DEFAULT ''")

        // investment_transaction — UUID doc ID + denormalized Firebase IDs for path resolution
        db.execSQL("ALTER TABLE `investment_transaction` ADD COLUMN `transactionFirebaseId` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `investment_transaction` ADD COLUMN `shopFirebaseId` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `investment_transaction` ADD COLUMN `investorFirebaseId` TEXT NOT NULL DEFAULT ''")

        // year_end_settlement — UUID doc ID
        db.execSQL("ALTER TABLE `year_end_settlement` ADD COLUMN `settlementFirebaseId` TEXT NOT NULL DEFAULT ''")

        // settlement_entry — UUID doc ID + denormalized investor Firebase ID
        db.execSQL("ALTER TABLE `settlement_entry` ADD COLUMN `entryFirebaseId` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `settlement_entry` ADD COLUMN `investorFirebaseId` TEXT NOT NULL DEFAULT ''")
    }
}
