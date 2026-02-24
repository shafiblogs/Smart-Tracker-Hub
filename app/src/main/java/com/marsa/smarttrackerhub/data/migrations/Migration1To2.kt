package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: Investment module added.
 *
 * New tables:
 *  - shop_investor          (junction: shop ↔ investor with share % and status)
 *  - investment_transaction (phase-based payments per shop-investor)
 *  - year_end_settlement    (period-based reconciliation event per shop)
 *  - settlement_entry       (per-investor line inside a settlement)
 *
 * Dropped tables:
 *  - entries                (EntryEntity removed from the domain)
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // ── Drop legacy entries table ─────────────────────────────────────
        db.execSQL("DROP TABLE IF EXISTS `entries`")

        // ── shop_investor ─────────────────────────────────────────────────
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `shop_investor` (
                `id`              INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `shopId`          INTEGER NOT NULL,
                `investorId`      INTEGER NOT NULL,
                `sharePercentage` REAL    NOT NULL,
                `status`          TEXT    NOT NULL DEFAULT 'Active',
                `joinedDate`      INTEGER NOT NULL,
                FOREIGN KEY(`shopId`)     REFERENCES `shop_info`(`id`)     ON DELETE CASCADE,
                FOREIGN KEY(`investorId`) REFERENCES `investor_info`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shop_investor_shopId`     ON `shop_investor` (`shopId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shop_investor_investorId` ON `shop_investor` (`investorId`)")

        // ── investment_transaction ────────────────────────────────────────
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `investment_transaction` (
                `id`              INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `shopInvestorId`  INTEGER NOT NULL,
                `amount`          REAL    NOT NULL,
                `transactionDate` INTEGER NOT NULL,
                `phase`           TEXT    NOT NULL,
                `note`            TEXT    NOT NULL DEFAULT '',
                FOREIGN KEY(`shopInvestorId`) REFERENCES `shop_investor`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_investment_transaction_shopInvestorId` ON `investment_transaction` (`shopInvestorId`)")

        // ── year_end_settlement ───────────────────────────────────────────
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `year_end_settlement` (
                `id`               INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `shopId`           INTEGER NOT NULL,
                `settlementDate`   INTEGER NOT NULL,
                `periodStartDate`  INTEGER NOT NULL DEFAULT 0,
                `totalInvested`    REAL    NOT NULL,
                `note`             TEXT    NOT NULL DEFAULT '',
                `isCarriedForward` INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY(`shopId`) REFERENCES `shop_info`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_year_end_settlement_shopId` ON `year_end_settlement` (`shopId`)")

        // ── settlement_entry ──────────────────────────────────────────────
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `settlement_entry` (
                `id`                    INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `settlementId`          INTEGER NOT NULL,
                `investorId`            INTEGER NOT NULL,
                `fairShareAmount`       REAL    NOT NULL,
                `actualPaidAmount`      REAL    NOT NULL,
                `balanceAmount`         REAL    NOT NULL,
                `settlementPaidAmount`  REAL    NOT NULL DEFAULT 0.0,
                `settlementPaidDate`    INTEGER,
                FOREIGN KEY(`settlementId`) REFERENCES `year_end_settlement`(`id`) ON DELETE CASCADE,
                FOREIGN KEY(`investorId`)   REFERENCES `investor_info`(`id`)       ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_settlement_entry_settlementId` ON `settlement_entry` (`settlementId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_settlement_entry_investorId`   ON `settlement_entry` (`investorId`)")
    }
}
