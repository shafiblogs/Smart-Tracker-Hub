package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.marsa.smarttrackerhub.data.entity.CachedLogEntry

@Dao
interface CachedLogEntryDao {

    @Query("SELECT * FROM cached_log_entries WHERE shopFirebaseId = :shopFirebaseId AND monthKey = :monthKey")
    suspend fun getForKey(shopFirebaseId: String, monthKey: String): List<CachedLogEntry>

    @Query("DELETE FROM cached_log_entries WHERE shopFirebaseId = :shopFirebaseId AND monthKey = :monthKey")
    suspend fun deleteForKey(shopFirebaseId: String, monthKey: String)

    @Insert
    suspend fun insertAll(entries: List<CachedLogEntry>)
}
