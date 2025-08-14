package com.marsa.smarttrackerhub.data.dao


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
