package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.marsa.smarttrackerhub.data.entity.AccountSummaryEntity

@Dao
interface AccountSummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountSummary(summary: AccountSummaryEntity)

    @Query("SELECT * FROM account_summary WHERE shopId = :shopId AND monthId = :monthId")
    suspend fun getAccountSummary(shopId: String, monthId: String): AccountSummaryEntity?

    @Query("SELECT * FROM account_summary WHERE shopId = :shopId ORDER BY monthTimestamp DESC")
    suspend fun getAllAccountSummariesForShop(shopId: String): List<AccountSummaryEntity>
}