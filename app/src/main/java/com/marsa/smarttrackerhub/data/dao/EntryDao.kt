package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.marsa.smarttrackerhub.data.entity.EntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Insert
    suspend fun insert(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: Int)

    @Update
    suspend fun update(entry: EntryEntity)

    @Query("SELECT * FROM entries WHERE date = :date")
    suspend fun getEntriesByDate(date: String): List<EntryEntity>

    @Query("SELECT DISTINCT date FROM entries")
    suspend fun getEntryDates(): List<String>

    @Query("SELECT DISTINCT date FROM entries WHERE date LIKE :month || '%' ORDER BY date ASC")
    fun getEntryDatesFlow(month: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM entries WHERE categoryId = :categoryId")
    suspend fun getEntryCountByCategory(categoryId: Int): Int

    @Query("SELECT COUNT(*) FROM entries WHERE vendorId = :vendorId")
    suspend fun getEntryCountByVendor(vendorId: Int): Int
}
