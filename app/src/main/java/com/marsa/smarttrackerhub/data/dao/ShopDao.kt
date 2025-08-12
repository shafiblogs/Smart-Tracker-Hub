package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import com.marsa.smarttrackerhub.data.entity.UserAccount


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

    @Query("SELECT * FROM shop_info")
    suspend fun getAllShops(): ShopInfo?

    @Update
    suspend fun updateShop(account: ShopInfo)
}