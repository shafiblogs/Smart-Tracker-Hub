package com.marsa.smarttrackerhub.data.repository

import com.marsa.smarttrackerhub.data.dao.ShopDao
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import kotlinx.coroutines.flow.Flow

class ShopRepository(private val shopDao: ShopDao) {

    fun getAllShops(): Flow<List<ShopInfo>> = shopDao.getAllShops()

    suspend fun getShopById(id: Int): ShopInfo? = shopDao.getShopById(id)

    // Local writes stamp updatedAt = now so newest-wins pull can propagate the edit.
    suspend fun insertShop(shop: ShopInfo) =
        shopDao.insertShop(shop.copy(updatedAt = System.currentTimeMillis()))

    suspend fun updateShop(shop: ShopInfo) =
        shopDao.updateShop(shop.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteShop(shop: ShopInfo) = shopDao.deleteShop(shop)

    suspend fun updateTotalInvested(shopId: Int, totalInvested: Double) =
        shopDao.updateTotalInvested(shopId, totalInvested, System.currentTimeMillis())

    /** Returns true if [shopId] is already used by another shop (excludes current record on edits). */
    suspend fun isShopIdTaken(shopId: String, excludeId: Int = 0): Boolean =
        shopDao.countByShopId(shopId, excludeId) > 0
}