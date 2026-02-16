package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.marsa.smarttrackerhub.data.entity.ShopInvestor
import com.marsa.smarttrackerhub.domain.InvestorShopDetail
import com.marsa.smarttrackerhub.domain.ShopInvestorDetail
import kotlinx.coroutines.flow.Flow

/**
 * Created by Muhammed Shafi on 16/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Dao
interface ShopInvestorDao {

    // Get all investors for a specific shop with details
    @Query("""
        SELECT 
            si.id as shopInvestorId,
            i.id as investorId,
            i.investorName,
            i.investorEmail,
            i.investorPhone,
            si.sharePercentage,
            si.investmentAmount,
            si.investmentDate
        FROM shop_investor si
        INNER JOIN investor_info i ON si.investorId = i.id
        WHERE si.shopId = :shopId
        ORDER BY si.sharePercentage DESC
    """)
    fun getInvestorsForShop(shopId: Int): Flow<List<ShopInvestorDetail>>

    // Get all shops for a specific investor with details
    @Query("""
        SELECT 
            si.id as shopInvestorId,
            s.id as shopId,
            s.shopName,
            s.shopAddress,
            si.sharePercentage,
            si.investmentAmount,
            si.investmentDate
        FROM shop_investor si
        INNER JOIN shop_info s ON si.shopId = s.id
        WHERE si.investorId = :investorId
        ORDER BY si.investmentDate DESC
    """)
    fun getShopsForInvestor(investorId: Int): Flow<List<InvestorShopDetail>>

    // Get total investment for a shop
    @Query("SELECT COALESCE(SUM(investmentAmount), 0) FROM shop_investor WHERE shopId = :shopId")
    suspend fun getTotalInvestmentForShop(shopId: Int): Double

    // Get total percentage allocated for a shop
    @Query("SELECT COALESCE(SUM(sharePercentage), 0) FROM shop_investor WHERE shopId = :shopId")
    suspend fun getTotalPercentageForShop(shopId: Int): Double

    // Get total invested by an investor across all shops
    @Query("SELECT COALESCE(SUM(investmentAmount), 0) FROM shop_investor WHERE investorId = :investorId")
    suspend fun getTotalInvestedByInvestor(investorId: Int): Double

    // Get shop count for an investor
    @Query("SELECT COUNT(*) FROM shop_investor WHERE investorId = :investorId")
    suspend fun getShopCountForInvestor(investorId: Int): Int

    // Get investor count for a shop
    @Query("SELECT COUNT(*) FROM shop_investor WHERE shopId = :shopId")
    suspend fun getInvestorCountForShop(shopId: Int): Int

    // Check if investor already exists in shop
    @Query("SELECT COUNT(*) FROM shop_investor WHERE shopId = :shopId AND investorId = :investorId")
    suspend fun isInvestorInShop(shopId: Int, investorId: Int): Int

    // Get specific shop-investor record
    @Query("SELECT * FROM shop_investor WHERE id = :id")
    suspend fun getShopInvestorById(id: Int): ShopInvestor?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShopInvestor(shopInvestor: ShopInvestor)

    @Update
    suspend fun updateShopInvestor(shopInvestor: ShopInvestor)

    @Delete
    suspend fun deleteShopInvestor(shopInvestor: ShopInvestor)

    @Query("DELETE FROM shop_investor WHERE id = :id")
    suspend fun deleteShopInvestorById(id: Int)
}