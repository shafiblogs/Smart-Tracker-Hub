package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.marsa.smarttrackerhub.data.entity.SummaryEntity

@Dao
interface SummaryDao {
    @Query("SELECT * FROM summary WHERE shopId = :shopId AND monthId = :monthId")
    suspend fun getSummary(shopId: String, monthId: String): SummaryEntity?

    @Query("SELECT * FROM summary WHERE shopId = :shopId ORDER BY monthTimestamp DESC")
    suspend fun getAllSummariesForShop(shopId: String): List<SummaryEntity>

    @Query("SELECT * FROM summary WHERE shopId = :shopId ORDER BY monthTimestamp ASC")
    suspend fun getAllSummariesForShopAscending(shopId: String): List<SummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: SummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummaries(summaries: List<SummaryEntity>)

    @Query("DELETE FROM summary WHERE shopId = :shopId")
    suspend fun deleteSummariesForShop(shopId: String)

    @Query("DELETE FROM summary WHERE lastUpdated < :timestamp")
    suspend fun deleteOldSummaries(timestamp: Long)

    @Query("DELETE FROM summary")
    suspend fun clearAll()

    @Query("""
    DELETE FROM summary
    WHERE monthId = :currentMonthId
    AND lastUpdated < :expiryTime
""")
    suspend fun deleteExpiredCurrentMonth(
        currentMonthId: String,
        expiryTime: Long
    )
}
