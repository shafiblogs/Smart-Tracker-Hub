package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import com.marsa.smarttrackerhub.data.entity.UserAccount
import kotlinx.coroutines.flow.Flow


/**
 * Created by Muhammed Shafi on 18/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Dao
interface ShopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: ShopInfo)

    @Query("SELECT COUNT(*) FROM shop_info")
    suspend fun hasShops(): Boolean

    @Update
    suspend fun updateShop(account: ShopInfo)

    @Query("SELECT * FROM shop_info ORDER BY shopName ASC")
    fun getAllShops(): Flow<List<ShopInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: ShopInfo)

    @Delete
    suspend fun deleteShop(shop: ShopInfo)
}