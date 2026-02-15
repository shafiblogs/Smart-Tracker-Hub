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
}