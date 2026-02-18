package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.marsa.smarttrackerhub.data.entity.InvestmentTransaction
import com.marsa.smarttrackerhub.domain.PhaseTransactionDetail
import kotlinx.coroutines.flow.Flow

/**
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Dao
interface InvestmentTransactionDao {

    /**
     * All transactions for a specific shop-investor link (for detail view / edit).
     */
    @Query("SELECT * FROM investment_transaction WHERE shopInvestorId = :shopInvestorId ORDER BY transactionDate DESC")
    fun getTransactionsForShopInvestor(shopInvestorId: Int): Flow<List<InvestmentTransaction>>

    /**
     * All transactions for a shop, joined with investor name and phase.
     * Used in ShopInvestmentDashboard phase breakdown list.
     */
    @Query("""
        SELECT
            t.id             AS transactionId,
            t.shopInvestorId,
            i.id             AS investorId,
            i.investorName,
            t.phase,
            t.amount,
            t.transactionDate,
            t.note
        FROM investment_transaction t
        INNER JOIN shop_investor si ON t.shopInvestorId = si.id
        INNER JOIN investor_info i  ON si.investorId = i.id
        WHERE si.shopId = :shopId
        ORDER BY t.transactionDate DESC
    """)
    fun getTransactionsForShop(shopId: Int): Flow<List<PhaseTransactionDetail>>

    /**
     * Transactions for a specific investor in a specific shop.
     * Used in settlement calculation.
     */
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM investment_transaction t
        INNER JOIN shop_investor si ON t.shopInvestorId = si.id
        WHERE si.shopId = :shopId AND si.investorId = :investorId
    """)
    suspend fun getTotalPaidByInvestorForShop(shopId: Int, investorId: Int): Double

    /**
     * Total paid into a shop by ALL investors.
     */
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM investment_transaction t
        INNER JOIN shop_investor si ON t.shopInvestorId = si.id
        WHERE si.shopId = :shopId
    """)
    suspend fun getTotalPaidForShop(shopId: Int): Double

    /**
     * Total paid by a specific shopInvestor entry.
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM investment_transaction WHERE shopInvestorId = :shopInvestorId")
    suspend fun getTotalPaidByShopInvestor(shopInvestorId: Int): Double

    /**
     * Transactions for a shop since a given date (for year-end period filtering).
     */
    @Query("""
        SELECT
            t.id             AS transactionId,
            t.shopInvestorId,
            i.id             AS investorId,
            i.investorName,
            t.phase,
            t.amount,
            t.transactionDate,
            t.note
        FROM investment_transaction t
        INNER JOIN shop_investor si ON t.shopInvestorId = si.id
        INNER JOIN investor_info i  ON si.investorId = i.id
        WHERE si.shopId = :shopId AND t.transactionDate >= :fromDate
        ORDER BY t.transactionDate ASC
    """)
    suspend fun getTransactionsForShopSince(shopId: Int, fromDate: Long): List<PhaseTransactionDetail>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: InvestmentTransaction): Long

    @Update
    suspend fun updateTransaction(transaction: InvestmentTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: InvestmentTransaction)

    @Query("DELETE FROM investment_transaction WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)
}
