package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v2 → v3: Consolidated production migration.
 *
 * Covers all schema changes introduced during development (previously spread
 * across intermediate migrations v3–v9). One migration file keeps the
 * upgrade path clean for devices on the v2 production build.
 *
 * Changes:
 *  shop_info:
 *    - `shopStatus`    TEXT    'Initial'   — shop lifecycle state
 *    - `totalInvested` REAL    0.0         — cached sum of investment amounts
 *    - `isSynced`      INTEGER 0           — Firebase push flag
 *    - `shopRegion`    TEXT    'UAE'        — UAE | KUWAIT | KSA
 *
 *  employee_info:
 *    - `employeeId`               TEXT '' — Firebase document ID
 *    - `associatedShopFirebaseId` TEXT '' — denormalized shop Firebase ID
 *    - `isSynced`                 INTEGER 0
 *
 *  investor_info:
 *    - `investorId` TEXT '' — Firebase document ID
 *    - `isSynced`   INTEGER 0
 *
 *  shop_investor:
 *    - `shopInvestorFirebaseId` TEXT '' — composite Firebase doc ID
 *    - `isSynced`               INTEGER 0
 *
 *  investment_transaction:
 *    - `transactionFirebaseId` TEXT '' — UUID Firebase doc ID
 *    - `shopFirebaseId`        TEXT '' — denormalized
 *    - `investorFirebaseId`    TEXT '' — denormalized
 *    - `isSynced`              INTEGER 0
 *
 *  year_end_settlement:
 *    - `settlementFirebaseId` TEXT '' — UUID Firebase doc ID
 *    - `shopFirebaseId`       TEXT '' — denormalized
 *    - `isSynced`             INTEGER 0
 *
 *  settlement_entry:
 *    - `entryFirebaseId`      TEXT '' — UUID Firebase doc ID
 *    - `investorFirebaseId`   TEXT '' — denormalized
 *    - `settlementFirebaseId` TEXT '' — parent settlement doc ID
 *    - `shopFirebaseId`       TEXT '' — denormalized
 *    - `isSynced`             INTEGER 0
 *
 *  Data fix:
 *    - Resets isSynced=0 for employees with blank employeeId so the
 *      sync worker re-processes them and assigns a proper UUID.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // ── shop_info ─────────────────────────────────────────────────────
        db.execSQL("ALTER TABLE `shop_info` ADD COLUMN `shopStatus`    TEXT    NOT NULL DEFAULT 'Initial'")
        db.execSQL("ALTER TABLE `shop_info` ADD COLUMN `totalInvested` REAL    NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE `shop_info` ADD COLUMN `isSynced`      INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `shop_info` ADD COLUMN `shopRegion`    TEXT    NOT NULL DEFAULT 'UAE'")

        // ── employee_info ─────────────────────────────────────────────────
        db.execSQL("ALTER TABLE `employee_info` ADD COLUMN `employeeId`               TEXT    NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `employee_info` ADD COLUMN `associatedShopFirebaseId` TEXT    NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `employee_info` ADD COLUMN `isSynced`                 INTEGER NOT NULL DEFAULT 0")

        // ── investor_info ─────────────────────────────────────────────────
        db.execSQL("ALTER TABLE `investor_info` ADD COLUMN `investorId` TEXT    NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `investor_info` ADD COLUMN `isSynced`   INTEGER NOT NULL DEFAULT 0")

        // ── shop_investor ─────────────────────────────────────────────────
        db.execSQL("ALTER TABLE `shop_investor` ADD COLUMN `shopInvestorFirebaseId` TEXT    NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `shop_investor` ADD COLUMN `isSynced`               INTEGER NOT NULL DEFAULT 0")

        // ── investment_transaction ────────────────────────────────────────
        db.execSQL("ALTER TABLE `investment_transaction` ADD COLUMN `transactionFirebaseId` TEXT    NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `investment_transaction` ADD COLUMN `shopFirebaseId`        TEXT    NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `investment_transaction` ADD COLUMN `investorFirebaseId`    TEXT    NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `investment_transaction` ADD COLUMN `isSynced`              INTEGER NOT NULL DEFAULT 0")

        // ── year_end_settlement ───────────────────────────────────────────
        db.execSQL("ALTER TABLE `year_end_settlement` ADD COLUMN `settlementFirebaseId` TEXT    NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `year_end_settlement` ADD COLUMN `shopFirebaseId`       TEXT    NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `year_end_settlement` ADD COLUMN `isSynced`             INTEGER NOT NULL DEFAULT 0")

        // ── settlement_entry ──────────────────────────────────────────────
        db.execSQL("ALTER TABLE `settlement_entry` ADD COLUMN `entryFirebaseId`      TEXT    NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `settlement_entry` ADD COLUMN `investorFirebaseId`   TEXT    NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `settlement_entry` ADD COLUMN `settlementFirebaseId` TEXT    NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `settlement_entry` ADD COLUMN `shopFirebaseId`       TEXT    NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `settlement_entry` ADD COLUMN `isSynced`             INTEGER NOT NULL DEFAULT 0")

        // ── Data fix: re-queue employees that never got a Firebase ID ─────
        db.execSQL("UPDATE `employee_info` SET `isSynced` = 0 WHERE `employeeId` = ''")
    }
}
