package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.marsa.smarttrackerhub.data.entity.SaleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SaleEntity)

    @Query("SELECT * FROM sales WHERE date LIKE :month || '%' ORDER BY date ASC")
    fun getAllSalesFlow(month: String): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE date = :date")
    suspend fun getSalesByDate(date: String): SaleEntity?

    @Query("SELECT IFNULL(SUM(creditSale), 0.0) FROM sales WHERE date <= :date")
    suspend fun getTotalCreditSalesUpTo(date: String): Double

    @Query("SELECT IFNULL(SUM(cashPayment + cardPayment), 0.0) FROM sales WHERE date <= :date")
    suspend fun getTotalCreditSalePaymentsUpTo(date: String): Double

    @Query("SELECT DISTINCT date FROM sales")
    suspend fun getSalesDates(): List<String>

    @Query("SELECT DISTINCT date FROM sales WHERE date LIKE :month || '%' ORDER BY date ASC")
    fun getSalesDatesFlow(month: String): Flow<List<String>>

    @Query("DELETE FROM sales WHERE date = :date")
    suspend fun deleteEntryByDate(date: String)
}
