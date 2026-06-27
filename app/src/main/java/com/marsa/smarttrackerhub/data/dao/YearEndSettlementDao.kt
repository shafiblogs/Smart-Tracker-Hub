package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.marsa.smarttrackerhub.data.entity.SettlementEntry
import com.marsa.smarttrackerhub.data.entity.YearEndSettlement
import kotlinx.coroutines.flow.Flow

/**
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Dao
interface YearEndSettlementDao {

    /** All settlements for a shop, most recent first. */
    @Query("SELECT * FROM year_end_settlement WHERE shopId = :shopId ORDER BY settlementDate DESC")
    fun getSettlementsForShop(shopId: Int): Flow<List<YearEndSettlement>>

    /** Latest settlement for a shop (to determine period start for next calculation). */
    @Query("SELECT * FROM year_end_settlement WHERE shopId = :shopId ORDER BY settlementDate DESC LIMIT 1")
    suspend fun getLatestSettlement(shopId: Int): YearEndSettlement?

    /** Single settlement by Room primary key — used by sync to resolve a parent settlement. */
    @Query("SELECT * FROM year_end_settlement WHERE id = :id LIMIT 1")
    suspend fun getSettlementById(id: Int): YearEndSettlement?

    /** All investor entries for a specific settlement. */
    @Query("""
        SELECT se.*, i.investorName
        FROM settlement_entry se
        INNER JOIN investor_info i ON se.investorId = i.id
        WHERE se.settlementId = :settlementId
        ORDER BY se.balanceAmount DESC
    """)
    fun getSettlementEntries(settlementId: Int): Flow<List<SettlementEntryWithName>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: YearEndSettlement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlementEntries(entries: List<SettlementEntry>)

    @Update
    suspend fun updateSettlement(settlement: YearEndSettlement)

    @Update
    suspend fun updateSettlementEntry(entry: SettlementEntry)

    @Query("DELETE FROM year_end_settlement WHERE id = :id")
    suspend fun deleteSettlement(id: Int)

    // ── Firebase sync ──────────────────────────────────────────────────────────

    /** All settlements not yet pushed to Firestore. */
    @Query("SELECT * FROM year_end_settlement WHERE isSynced = 0")
    suspend fun getUnsyncedSettlements(): List<YearEndSettlement>

    /** Marks the settlement with the given [settlementFirebaseId] (UUID) as synced. */
    @Query("UPDATE year_end_settlement SET isSynced = 1 WHERE settlementFirebaseId = :settlementFirebaseId")
    suspend fun markSettlementSynced(settlementFirebaseId: String)

    /** All settlement entries not yet pushed to Firestore. */
    @Query("SELECT * FROM settlement_entry WHERE isSynced = 0")
    suspend fun getUnsyncedSettlementEntries(): List<SettlementEntry>

    /** Marks the settlement entry with the given [entryFirebaseId] (UUID) as synced. */
    @Query("UPDATE settlement_entry SET isSynced = 1 WHERE entryFirebaseId = :entryFirebaseId")
    suspend fun markSettlementEntrySynced(entryFirebaseId: String)

    /** Force-resync support: re-queue every settlement and entry. */
    @Query("UPDATE year_end_settlement SET isSynced = 0")
    suspend fun markAllSettlementsUnsynced()

    @Query("UPDATE settlement_entry SET isSynced = 0")
    suspend fun markAllSettlementEntriesUnsynced()

    /** Entries for one settlement — used by delete to gather Firestore doc IDs before cascade. */
    @Query("SELECT * FROM settlement_entry WHERE settlementId = :settlementId")
    suspend fun getSettlementEntriesList(settlementId: Int): List<SettlementEntry>

    // ── Duplicate cleanup (local only) ──
    @Query("UPDATE settlement_entry SET investorId = :keepId WHERE investorId = :dupId")
    suspend fun repointEntryInvestor(dupId: Int, keepId: Int)

    @Query("UPDATE settlement_entry SET settlementId = :keepId WHERE settlementId = :dupId")
    suspend fun repointEntrySettlement(dupId: Int, keepId: Int)

    @Query("DELETE FROM settlement_entry WHERE id = :id")
    suspend fun deleteSettlementEntryById(id: Int)

    // ── Pull support ───────────────────────────────────────────────────────────

    /** One-shot list of all settlements — used by pull to build Firebase-id → Room-id map. */
    @Query("SELECT * FROM year_end_settlement")
    suspend fun getAllSettlementsList(): List<YearEndSettlement>

    /** One-shot list of all settlement entries — used by pull to detect existing records. */
    @Query("SELECT * FROM settlement_entry")
    suspend fun getAllSettlementEntriesList(): List<SettlementEntry>

    /** Look up a settlement by Firebase string ID — used after pull-insert to get Room int PK. */
    @Query("SELECT * FROM year_end_settlement WHERE settlementFirebaseId = :firebaseId LIMIT 1")
    suspend fun getSettlementByFirebaseId(firebaseId: String): YearEndSettlement?

    /** Single-entry insert — used by pull (bulk insert with list also works). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlementEntry(entry: SettlementEntry): Long

    /** Delete by Firebase id — used by pulled tombstones. */
    @Query("DELETE FROM year_end_settlement WHERE settlementFirebaseId = :firebaseId")
    suspend fun deleteSettlementByFirebaseId(firebaseId: String)

    @Query("DELETE FROM settlement_entry WHERE entryFirebaseId = :firebaseId")
    suspend fun deleteEntryByFirebaseId(firebaseId: String)
}

/** Projection joining settlement_entry with investor name — used for history display. */
data class SettlementEntryWithName(
    val id: Int,
    val settlementId: Int,
    val investorId: Int,
    val investorName: String,
    val fairShareAmount: Double,
    val actualPaidAmount: Double,
    val balanceAmount: Double,
    val settlementPaidAmount: Double,
    val settlementPaidDate: Long?,
    val entryFirebaseId: String = "",        // Preserved so markEntrySettled can pass it through
    val investorFirebaseId: String = "",     // Preserved so markEntrySettled can pass it through
    val settlementFirebaseId: String = "",   // Preserved so markEntrySettled can pass it through
    val shopFirebaseId: String = ""          // Preserved so markEntrySettled can pass it through
)
