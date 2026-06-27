package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v5 → v6 — the single post-v5 (last released) migration. Bundles every schema change made
 * after v5, since v6 was never released:
 *
 *  1. De-duplicate the purchase_items cache and enforce a UNIQUE business key. A writer was
 *     appending a fresh copy of each category on every load, so the Purchase screen showed
 *     duplicates. The table is fully re-derivable from Firestore, so we clear it and add a
 *     UNIQUE index on (shopId, monthId, categoryId); @Insert(REPLACE) then collapses repeats.
 *  2. Add `updatedAt` to the 7 Hub-synced tables for newest-wins edit propagation. Default 0
 *     = "baseline/unknown": existing rows and pre-timestamp Firestore docs compare equal so
 *     neither overwrites the other; the first real edit stamps now() and wins.
 *  3. Create the `deletions` tombstone table for cross-device delete propagation.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1 — purchase_items de-dup: clear (re-fetched from Firestore) + UNIQUE business key.
        database.execSQL("DELETE FROM purchase_items")
        database.execSQL("DROP INDEX IF EXISTS idx_purchase_items_shopId_monthId")
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_purchase_items_shopId_monthId_categoryId " +
                "ON purchase_items (shopId, monthId, categoryId)"
        )

        // 2 — newest-wins: add updatedAt to the 7 Hub-synced tables.
        listOf(
            "shop_info",
            "employee_info",
            "investor_info",
            "shop_investor",
            "investment_transaction",
            "year_end_settlement",
            "settlement_entry"
        ).forEach { table ->
            database.execSQL("ALTER TABLE `$table` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        }

        // 3 — tombstones for cross-device delete propagation.
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `deletions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `collection` TEXT NOT NULL,
                `firebaseId` TEXT NOT NULL,
                `deletedAt` INTEGER NOT NULL,
                `isSynced` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_deletions_collection_firebaseId` ON `deletions` (`collection`, `firebaseId`)"
        )
    }
}
