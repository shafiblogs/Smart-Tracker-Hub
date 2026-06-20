package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.marsa.smarttrackerhub.data.entity.PurchaseEntity

@Dao
interface PurchaseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchases(purchases: List<PurchaseEntity>)

    @Query("SELECT * FROM purchase_items WHERE shopId = :shopId AND monthId = :monthId ORDER BY totalAmount DESC")
    suspend fun getPurchasesForMonth(shopId: String, monthId: String): List<PurchaseEntity>

    @Query("DELETE FROM purchase_items WHERE shopId = :shopId AND monthId = :monthId")
    suspend fun deletePurchasesForMonth(shopId: String, monthId: String)

    @Query("DELETE FROM purchase_items WHERE shopId = :shopId")
    suspend fun deletePurchasesForShop(shopId: String)
}
