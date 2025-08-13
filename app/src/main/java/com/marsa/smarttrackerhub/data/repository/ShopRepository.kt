package com.marsa.smarttrackerhub.data.repository

import com.marsa.smarttrackerhub.data.dao.ShopDao
import com.marsa.smarttrackerhub.data.dao.UserAccountDao
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import com.marsa.smarttrackerhub.data.entity.UserAccount
import kotlinx.coroutines.flow.Flow


/**
 * Created by Muhammed Shafi on 18/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class ShopRepository(private val dao: ShopDao) {

    suspend fun updateShop(account: ShopInfo) =
        dao.updateShop(account)

    suspend fun insertShop(account: ShopInfo) =
        dao.insert(account)

    suspend fun hasShops(): Boolean = dao.hasShops()

    fun getAllShops(): Flow<List<ShopInfo>> = dao.getAllShops()
}
