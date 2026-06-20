package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create purchase_items table for caching purchase breakdown data
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS purchase_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                shopId TEXT NOT NULL,
                monthId TEXT NOT NULL,
                categoryId INTEGER NOT NULL,
                categoryName TEXT NOT NULL,
                totalAmount REAL NOT NULL,
                lastUpdated INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Create index for fast lookups by shopId and monthId
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_purchase_items_shopId_monthId ON purchase_items(shopId, monthId)"
        )
    }
}
