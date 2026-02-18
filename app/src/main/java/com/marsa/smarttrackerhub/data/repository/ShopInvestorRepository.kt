package com.marsa.smarttrackerhub.data.repository

import com.marsa.smarttrackerhub.data.dao.ShopInvestorDao
import com.marsa.smarttrackerhub.data.entity.ShopInvestor
import com.marsa.smarttrackerhub.domain.InvestorShopSummary
import com.marsa.smarttrackerhub.domain.ShopInvestorSummary
import kotlinx.coroutines.flow.Flow

/**
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class ShopInvestorRepository(private val shopInvestorDao: ShopInvestorDao) {

    fun getInvestorsForShop(shopId: Int): Flow<List<ShopInvestorSummary>> =
        shopInvestorDao.getInvestorsForShop(shopId)

    fun getShopsForInvestor(investorId: Int): Flow<List<InvestorShopSummary>> =
        shopInvestorDao.getShopsForInvestor(investorId)

    suspend fun getTotalPercentageForShop(shopId: Int): Double =
        shopInvestorDao.getTotalPercentageForShop(shopId)

    suspend fun getInvestorCountForShop(shopId: Int): Int =
        shopInvestorDao.getInvestorCountForShop(shopId)

    suspend fun getShopCountForInvestor(investorId: Int): Int =
        shopInvestorDao.getShopCountForInvestor(investorId)

    suspend fun getTotalPaidByInvestor(investorId: Int): Double =
        shopInvestorDao.getTotalPaidByInvestor(investorId)

    suspend fun isInvestorInShop(shopId: Int, investorId: Int): Boolean =
        shopInvestorDao.isInvestorInShop(shopId, investorId) > 0

    suspend fun getShopInvestorById(id: Int): ShopInvestor? =
        shopInvestorDao.getShopInvestorById(id)

    suspend fun getActiveInvestorsRaw(shopId: Int): List<ShopInvestor> =
        shopInvestorDao.getActiveInvestorsRaw(shopId)

    suspend fun insertShopInvestor(shopInvestor: ShopInvestor): Long =
        shopInvestorDao.insertShopInvestor(shopInvestor)

    suspend fun updateShopInvestor(shopInvestor: ShopInvestor) =
        shopInvestorDao.updateShopInvestor(shopInvestor)

    suspend fun deleteShopInvestor(shopInvestor: ShopInvestor) =
        shopInvestorDao.deleteShopInvestor(shopInvestor)

    suspend fun deleteShopInvestorById(id: Int) =
        shopInvestorDao.deleteShopInvestorById(id)
}
