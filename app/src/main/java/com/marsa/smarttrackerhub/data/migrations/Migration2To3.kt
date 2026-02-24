package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v2 → v3: Settlement period-based redesign.
 *
 * Changes to `year_end_settlement`:
 *  - Remove `year` INTEGER column (calendar-year key no longer used)
 *  - Add `periodStartDate` INTEGER column (explicit period start timestamp;
 *    0 = "from the beginning" for the first settlement)
 *
 * SQLite does not support DROP COLUMN directly (pre-API 35), so we
 * recreate the table with the new schema and copy existing data across.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // 1. Create the new table with the v3 schema
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `year_end_settlement_new` (
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

        // 2. Copy existing rows — map `year` → periodStartDate as 0
        //    (period start is unknown for legacy rows; treat as "all time")
        db.execSQL(
            """
            INSERT INTO `year_end_settlement_new`
                (`id`, `shopId`, `settlementDate`, `periodStartDate`, `totalInvested`, `note`, `isCarriedForward`)
            SELECT
                `id`, `shopId`, `settlementDate`, 0, `totalInvested`, `note`, `isCarriedForward`
            FROM `year_end_settlement`
            """.trimIndent()
        )

        // 3. Drop the old table
        db.execSQL("DROP TABLE `year_end_settlement`")

        // 4. Rename the new table
        db.execSQL("ALTER TABLE `year_end_settlement_new` RENAME TO `year_end_settlement`")

        // 5. Recreate the index (dropped with the old table)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_year_end_settlement_shopId` ON `year_end_settlement` (`shopId`)")
    }
}
