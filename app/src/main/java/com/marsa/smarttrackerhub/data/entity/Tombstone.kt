package com.marsa.smarttrackerhub.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A deletion marker. When a record is hard-deleted locally we also write one of these so the
 * delete propagates: push removes the Firestore doc + writes a `/deletions` doc, and other
 * devices' pull removes their local copy. Makes deletes reliable (retried by the worker) and
 * cross-device.
 *
 * `collection` is the Firestore collection name (shops, employees, investors, shop_investors,
 * transactions, settlements, settlement_entries); `firebaseId` is that doc's id.
 */
@Entity(
    tableName = "deletions",
    indices = [Index(value = ["collection", "firebaseId"], unique = true)]
)
data class Tombstone(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val collection: String,
    val firebaseId: String,
    val deletedAt: Long,
    val isSynced: Boolean = false
)
