package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.marsa.smarttrackerhub.data.entity.Tombstone

@Dao
interface TombstoneDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tombstone: Tombstone)

    @Query("SELECT * FROM deletions WHERE isSynced = 0")
    suspend fun getUnsynced(): List<Tombstone>

    @Query("UPDATE deletions SET isSynced = 1 WHERE collection = :collection AND firebaseId = :firebaseId")
    suspend fun markSynced(collection: String, firebaseId: String)
}
