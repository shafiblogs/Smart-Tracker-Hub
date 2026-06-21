package com.marsa.smarttrackerhub.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v4 → v5 — Re-queue investor-domain rows that were silently flagged "synced" without
 * ever being pushed to Firestore.
 *
 * Background: legacy investor/shop-investor/transaction/settlement rows carry a blank
 * Firebase ID (the ID columns were added blank in Migration2To3 with no backfill).
 * The old sync code hit a blank-ID branch and called markXSynced(""), i.e.
 * `UPDATE <table> SET isSynced = 1 WHERE <fbId> = ''`, which flagged ALL blank rows as
 * synced in one shot — so they never pushed. The sync layer now generates a Firebase ID
 * on blank instead of skipping (mirrors syncEmployee), but those rows are stuck at
 * isSynced = 1 and won't be picked up by syncAll().
 *
 * This migration resets isSynced = 0 wherever the Firebase ID is still blank so the next
 * sync re-attempts them. No schema/column changes — the columns exist since v3.
 *
 * Created by Claude on behalf of Muhammed Shafi.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE `investor_info`          SET `isSynced` = 0 WHERE `investorId`             = ''")
        database.execSQL("UPDATE `shop_investor`          SET `isSynced` = 0 WHERE `shopInvestorFirebaseId` = ''")
        database.execSQL("UPDATE `investment_transaction` SET `isSynced` = 0 WHERE `transactionFirebaseId`  = ''")
        database.execSQL("UPDATE `year_end_settlement`    SET `isSynced` = 0 WHERE `settlementFirebaseId`   = ''")
        database.execSQL("UPDATE `settlement_entry`       SET `isSynced` = 0 WHERE `entryFirebaseId`        = ''")
    }
}
