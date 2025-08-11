package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.marsa.smarttrackerhub.data.entity.SummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SummaryEntity)

    @Query("SELECT * FROM summary WHERE monthName = :month")
    suspend fun getSummary(month: String): SummaryEntity?

    @Query("SELECT * FROM summary WHERE monthName = :month")
    fun getSummaryFlow(month: String): Flow<SummaryEntity?>

    @Query("SELECT IFNULL(cashBalance, 0.0) FROM summary WHERE monthName = :month")
    suspend fun getCashBalanceByMonth(month: String): Double

    @Update
    suspend fun update(summary: SummaryEntity)

    @Query("SELECT monthName FROM summary")
    suspend fun getAllMonthNames(): List<String>

    @Query("SELECT monthName FROM summary ORDER BY monthName DESC LIMIT 1")
    suspend fun getLatestMonthName(): String
}
