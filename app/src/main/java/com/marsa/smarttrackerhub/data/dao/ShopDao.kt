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

    /** One-shot list for use in coroutines (e.g. mapping to ShopListDto). */
    @Query("SELECT * FROM shop_info")
    suspend fun getAllShopsAsList(): List<ShopInfo>

    /** Running shops only — used for selection dropdowns (excludes Initial and Closed). */
    @Query("SELECT * FROM shop_info WHERE shopStatus = 'Running'")
    suspend fun getActiveShopsAsList(): List<ShopInfo>

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

    // ── Firebase sync ──────────────────────────────────────────────────────────

    /** All shops not yet pushed to Firestore. */
    @Query("SELECT * FROM shop_info WHERE isSynced = 0")
    suspend fun getUnsyncedShops(): List<ShopInfo>

    /** Marks the shop with the given [shopId] string as synced.
     *  Using the Firebase string ID works for both new-insert (Room id=0) and update paths. */
    @Query("UPDATE shop_info SET isSynced = 1 WHERE shopId = :shopId")
    suspend fun markShopSynced(shopId: String)

    /** Marks ALL shops as unsynced so they are re-pushed to Firestore on the next sync.
     *  Use this after correcting shop data locally to force Firestore to receive the latest values. */
    @Query("UPDATE shop_info SET isSynced = 0")
    suspend fun markAllShopsUnsynced()

    // ── Pull support ───────────────────────────────────────────────────────────

    /** Look up a shop by its Firebase string ID — used after pull-insert to get the Room int PK. */
    @Query("SELECT * FROM shop_info WHERE shopId = :shopId LIMIT 1")
    suspend fun getShopByShopId(shopId: String): ShopInfo?
}