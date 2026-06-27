package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.marsa.smarttrackerhub.data.entity.InvestorInfo
import kotlinx.coroutines.flow.Flow

/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Dao
interface InvestorDao {

    @Query("SELECT * FROM investor_info ORDER BY investorName ASC")
    fun getAllInvestors(): Flow<List<InvestorInfo>>

    @Query("SELECT * FROM investor_info WHERE id = :id")
    suspend fun getInvestorById(id: Int): InvestorInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestor(investor: InvestorInfo)

    @Update
    suspend fun updateInvestor(investor: InvestorInfo)

    @Delete
    suspend fun deleteInvestor(investor: InvestorInfo)

    /** Returns the count of investors that already use [investorId], excluding [excludeId].
     *  Pass excludeId = 0 for new inserts. */
    @Query("SELECT COUNT(*) FROM investor_info WHERE investorId = :investorId AND id != :excludeId")
    suspend fun countByInvestorId(investorId: String, excludeId: Int): Int

    // ── Firebase sync ──────────────────────────────────────────────────────────

    /** All investors not yet pushed to Firestore. */
    @Query("SELECT * FROM investor_info WHERE isSynced = 0")
    suspend fun getUnsyncedInvestors(): List<InvestorInfo>

    /** Marks the investor with the given [investorId] string as synced. */
    @Query("UPDATE investor_info SET isSynced = 1 WHERE investorId = :investorId")
    suspend fun markInvestorSynced(investorId: String)

    /** Force-resync support: re-queue every investor. */
    @Query("UPDATE investor_info SET isSynced = 0")
    suspend fun markAllInvestorsUnsynced()

    // ── Pull support ───────────────────────────────────────────────────────────

    /** One-shot list used by pull to build a Firebase-id → Room-id map. */
    @Query("SELECT * FROM investor_info")
    suspend fun getAllInvestorsAsList(): List<InvestorInfo>

    /** Look up an investor by Firebase string ID — used after pull-insert to get the Room int PK. */
    @Query("SELECT * FROM investor_info WHERE investorId = :investorId LIMIT 1")
    suspend fun getInvestorByInvestorId(investorId: String): InvestorInfo?

    /** Delete by Firebase id — used by pulled tombstones (cascades to links/entries). */
    @Query("DELETE FROM investor_info WHERE investorId = :investorId AND updatedAt <= :deletedAt")
    suspend fun deleteByFirebaseId(investorId: String, deletedAt: Long)
}