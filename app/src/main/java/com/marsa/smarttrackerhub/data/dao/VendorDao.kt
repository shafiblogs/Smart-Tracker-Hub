package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.marsa.smarttrackerhub.data.entity.Vendor


/**
 * Created by Muhammed Shafi on 28/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Dao
interface VendorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendor(vendor: Vendor)

    @Update
    suspend fun updateVendor(vendor: Vendor)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(vendors: List<Vendor>)

    @Query("SELECT * FROM vendors ORDER BY name ASC")
    suspend fun getAllVendors(): List<Vendor>

    @Query("SELECT * FROM vendors WHERE id = :vendorId LIMIT 1")
    suspend fun getVendorById(vendorId: Int): Vendor?

    @Query("SELECT * FROM vendors WHERE categoryId = :categoryId ORDER BY name ASC")
    suspend fun getVendorsByCategory(categoryId: Int): List<Vendor>

    @Query("DELETE FROM vendors WHERE id = :vendorId")
    suspend fun deleteVendorById(vendorId: Int)

    @Query("SELECT COUNT(*) FROM vendors WHERE categoryId = :categoryId")
    suspend fun getVendorCountByCategory(categoryId: Int): Int
}