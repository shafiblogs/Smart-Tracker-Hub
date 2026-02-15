package com.marsa.smarttrackerhub.data.repository

import com.marsa.smarttrackerhub.data.dao.ShopDao
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import kotlinx.coroutines.flow.Flow

class ShopRepository(private val shopDao: ShopDao) {

    fun getAllShops(): Flow<List<ShopInfo>> = shopDao.getAllShops()

    suspend fun getShopById(id: Int): ShopInfo? = shopDao.getShopById(id)

    suspend fun insertShop(shop: ShopInfo) = shopDao.insertShop(shop)

    suspend fun updateShop(shop: ShopInfo) = shopDao.updateShop(shop)

    suspend fun deleteShop(shop: ShopInfo) = shopDao.deleteShop(shop)
}