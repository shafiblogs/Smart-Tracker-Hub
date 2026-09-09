package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.marsa.smarttrackerhub.data.entity.SyncMarker

@Dao
interface SyncMarkerDao {

    @Query("SELECT * FROM sync_markers WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): SyncMarker?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(marker: SyncMarker)
}
