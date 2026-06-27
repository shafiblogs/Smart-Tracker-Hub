package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v5 → v6 — De-duplicate the purchase_items cache and enforce a UNIQUE business key.
 *
 * purchase_items is a cache of Firestore `purchaseBreakdown`. A writer was appending a
 * fresh copy of each category on every load (auto-increment PK + non-unique index), so the
 * Purchase screen showed duplicates. Since the table is fully re-derivable from Firestore,
 * we simply clear it and add a UNIQUE index on (shopId, monthId, categoryId); from then on
 * @Insert(REPLACE) collapses repeats onto one row. The cache repopulates on the next load.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Clear the cache (harmless — re-fetched from Firestore) to remove existing duplicates.
        database.execSQL("DELETE FROM purchase_items")
        // Drop the old non-unique index if present, then add the UNIQUE business-key index.
        database.execSQL("DROP INDEX IF EXISTS idx_purchase_items_shopId_monthId")
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_purchase_items_shopId_monthId_categoryId " +
                "ON purchase_items (shopId, monthId, categoryId)"
        )
    }
}
