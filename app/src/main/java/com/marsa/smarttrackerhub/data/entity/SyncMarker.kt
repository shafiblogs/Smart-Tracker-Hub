package com.marsa.smarttrackerhub.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks, per sync target, when we last pulled from Firestore — so a pull can fetch only
 * changed docs (incremental) and fall back to a full reconcile periodically, and a repeat
 * refresh within the TTL window can skip the network call entirely.
 *
 * key examples: "shops", "investors", "trackerhub_sync:all", "trackerhub_sync:shops",
 *               "logs:{shopFirebaseId}:{yyyy-MM}"
 */
@Entity(tableName = "sync_markers")
data class SyncMarker(
    @PrimaryKey val key: String,
    // MAX(updatedAt) actually seen in remote docs — the incremental cursor. Stored as remote
    // time, so device clock skew never advances it incorrectly.
    @ColumnInfo(defaultValue = "0") val lastSeenRemoteMs: Long = 0L,
    // Local wall-clock of the last pull of ANY kind — the TTL/debounce anchor.
    @ColumnInfo(defaultValue = "0") val lastPullMs: Long = 0L,
    // Local wall-clock of the last FULL-collection pull — the reconcile-interval anchor.
    @ColumnInfo(defaultValue = "0") val lastFullPullMs: Long = 0L
)
