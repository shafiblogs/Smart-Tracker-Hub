package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    @Query("SELECT * FROM shop_info")
    fun getAllShops(): Flow<List<ShopInfo>>

    @Query("SELECT * FROM shop_info WHERE id = :id")
    suspend fun getShopById(id: Int): ShopInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: ShopInfo)

    @Update
    suspend fun updateShop(shop: ShopInfo)

    @Delete
    suspend fun deleteShop(shop: ShopInfo)

    @Query("UPDATE shop_info SET totalInvested = :totalInvested WHERE id = :shopId")
    suspend fun updateTotalInvested(shopId: Int, totalInvested: Double)

    /** Returns the count of shops that already use [shopId], excluding the record with [excludeId].
     *  Pass excludeId = 0 for new inserts (auto-generated IDs start at 1, so 0 never matches). */
    @Query("SELECT COUNT(*) FROM shop_info WHERE shopId = :shopId AND id != :excludeId")
    suspend fun countByShopId(shopId: String, excludeId: Int): Int
}