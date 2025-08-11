package com.marsa.smarttrackerhub.data.dao


/**
 * Created by Muhammed Shafi on 20/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.marsa.smarttrackerhub.data.entity.Category

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategories(): List<Category>

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Int)

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Int): Category?

    @Query("SELECT * FROM categories WHERE screenType = :screenType ORDER BY name ASC")
    suspend fun getCategoriesByType(screenType: String): List<Category>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<Category>)
}