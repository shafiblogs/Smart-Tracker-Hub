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
}