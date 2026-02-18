package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.marsa.smarttrackerhub.data.entity.ShopInvestor
import com.marsa.smarttrackerhub.domain.InvestorShopSummary
import com.marsa.smarttrackerhub.domain.ShopInvestorSummary
import kotlinx.coroutines.flow.Flow

/**
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Dao
interface ShopInvestorDao {

    /**
     * All investors in a shop with their total paid amount (SUM of all transactions).
     * Used in ShopInvestmentDashboard.
     */
    @Query("""
        SELECT
            si.id            AS shopInvestorId,
            i.id             AS investorId,
            i.investorName,
            si.sharePercentage,
            COALESCE(SUM(t.amount), 0) AS totalPaid,
            si.status,
            si.joinedDate
        FROM shop_investor si
        INNER JOIN investor_info i ON si.investorId = i.id
        LEFT JOIN investment_transaction t ON t.shopInvestorId = si.id
        WHERE si.shopId = :shopId
        GROUP BY si.id
        ORDER BY si.sharePercentage DESC
    """)
    fun getInvestorsForShop(shopId: Int): Flow<List<ShopInvestorSummary>>

    /**
     * All shops an investor is in, with total paid in each shop.
     * Used in InvestorDetailScreen.
     */
    @Query("""
        SELECT
            si.id            AS shopInvestorId,
            s.id             AS shopId,
            s.shopName,
            s.shopAddress,
            si.sharePercentage,
            COALESCE(SUM(t.amount), 0) AS totalPaid,
            si.status,
            si.joinedDate
        FROM shop_investor si
        INNER JOIN shop_info s ON si.shopId = s.id
        LEFT JOIN investment_transaction t ON t.shopInvestorId = si.id
        WHERE si.investorId = :investorId
        GROUP BY si.id
        ORDER BY si.joinedDate DESC
    """)
    fun getShopsForInvestor(investorId: Int): Flow<List<InvestorShopSummary>>

    /** Total share % already allocated for a shop (to validate new entries ≤ 100%). */
    @Query("SELECT COALESCE(SUM(sharePercentage), 0) FROM shop_investor WHERE shopId = :shopId AND status = 'Active'")
    suspend fun getTotalPercentageForShop(shopId: Int): Double

    /** Number of active investors in a shop. */
    @Query("SELECT COUNT(*) FROM shop_investor WHERE shopId = :shopId AND status = 'Active'")
    suspend fun getInvestorCountForShop(shopId: Int): Int

    /** Number of shops this investor is active in. */
    @Query("SELECT COUNT(*) FROM shop_investor WHERE investorId = :investorId AND status = 'Active'")
    suspend fun getShopCountForInvestor(investorId: Int): Int

    /** Total paid by this investor across ALL shops (for investor list card). */
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM investment_transaction t
        INNER JOIN shop_investor si ON t.shopInvestorId = si.id
        WHERE si.investorId = :investorId
    """)
    suspend fun getTotalPaidByInvestor(investorId: Int): Double

    /** Check if investor is already assigned to this shop. */
    @Query("SELECT COUNT(*) FROM shop_investor WHERE shopId = :shopId AND investorId = :investorId")
    suspend fun isInvestorInShop(shopId: Int, investorId: Int): Int

    /** Get specific record by id. */
    @Query("SELECT * FROM shop_investor WHERE id = :id")
    suspend fun getShopInvestorById(id: Int): ShopInvestor?

    /** Get all ShopInvestor records for a shop (raw, for settlement calculations). */
    @Query("SELECT * FROM shop_investor WHERE shopId = :shopId AND status = 'Active'")
    suspend fun getActiveInvestorsRaw(shopId: Int): List<ShopInvestor>

    /**
     * Active investors who joined on or before [asOfDate].
     * Used in period-based settlement so a new investor added after the period
     * is NOT included in earlier settlements.
     */
    @Query("SELECT * FROM shop_investor WHERE shopId = :shopId AND status = 'Active' AND joinedDate <= :asOfDate")
    suspend fun getActiveInvestorsAsOf(shopId: Int, asOfDate: Long): List<ShopInvestor>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShopInvestor(shopInvestor: ShopInvestor): Long

    @Update
    suspend fun updateShopInvestor(shopInvestor: ShopInvestor)

    @Delete
    suspend fun deleteShopInvestor(shopInvestor: ShopInvestor)

    @Query("DELETE FROM shop_investor WHERE id = :id")
    suspend fun deleteShopInvestorById(id: Int)
}
