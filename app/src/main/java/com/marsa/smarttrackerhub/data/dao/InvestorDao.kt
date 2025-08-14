package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.marsa.smarttrackerhub.data.entity.InvestorInfo
import kotlinx.coroutines.flow.Flow


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Dao
interface InvestorDao {

    @Query("SELECT * FROM investor_info")
    fun getAllInvestors(): Flow<List<InvestorInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestor(investor: InvestorInfo)

    @Delete
    suspend fun deleteInvestor(investor: InvestorInfo)
}
