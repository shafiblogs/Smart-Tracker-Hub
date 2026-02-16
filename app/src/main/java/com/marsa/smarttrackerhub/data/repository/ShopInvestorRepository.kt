package com.marsa.smarttrackerhub.data.repository

import com.marsa.smarttrackerhub.data.dao.ShopInvestorDao
import com.marsa.smarttrackerhub.data.entity.ShopInvestor
import com.marsa.smarttrackerhub.domain.InvestorShopDetail
import com.marsa.smarttrackerhub.domain.ShopInvestorDetail
import kotlinx.coroutines.flow.Flow

/**
 * Created by Muhammed Shafi on 16/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class ShopInvestorRepository(private val shopInvestorDao: ShopInvestorDao) {

    fun getInvestorsForShop(shopId: Int): Flow<List<ShopInvestorDetail>> =
        shopInvestorDao.getInvestorsForShop(shopId)

    fun getShopsForInvestor(investorId: Int): Flow<List<InvestorShopDetail>> =
        shopInvestorDao.getShopsForInvestor(investorId)

    suspend fun getTotalInvestmentForShop(shopId: Int): Double =
        shopInvestorDao.getTotalInvestmentForShop(shopId)

    suspend fun getTotalPercentageForShop(shopId: Int): Double =
        shopInvestorDao.getTotalPercentageForShop(shopId)

    suspend fun getTotalInvestedByInvestor(investorId: Int): Double =
        shopInvestorDao.getTotalInvestedByInvestor(investorId)

    suspend fun getShopCountForInvestor(investorId: Int): Int =
        shopInvestorDao.getShopCountForInvestor(investorId)

    suspend fun getInvestorCountForShop(shopId: Int): Int =
        shopInvestorDao.getInvestorCountForShop(shopId)

    suspend fun isInvestorInShop(shopId: Int, investorId: Int): Boolean =
        shopInvestorDao.isInvestorInShop(shopId, investorId) > 0

    suspend fun getShopInvestorById(id: Int): ShopInvestor? =
        shopInvestorDao.getShopInvestorById(id)

    suspend fun insertShopInvestor(shopInvestor: ShopInvestor) =
        shopInvestorDao.insertShopInvestor(shopInvestor)

    suspend fun updateShopInvestor(shopInvestor: ShopInvestor) =
        shopInvestorDao.updateShopInvestor(shopInvestor)

    suspend fun deleteShopInvestor(shopInvestor: ShopInvestor) =
        shopInvestorDao.deleteShopInvestor(shopInvestor)

    suspend fun deleteShopInvestorById(id: Int) =
        shopInvestorDao.deleteShopInvestorById(id)
}