package com.marsa.smarttrackerhub.data.repository

import com.marsa.smarttrackerhub.data.dao.SyncMarkerDao
import com.marsa.smarttrackerhub.data.entity.SyncMarker

/**
 * Single source of truth for "should this pull hit Firestore, and how much?".
 * Ported from AccountsTracker's SyncPolicy (same shape) — every pull routes through here so
 * the incremental-vs-full decision, the skew buffer, and the TTL guard stay identical
 * everywhere they're used.
 */
class SyncPolicy(private val dao: SyncMarkerDao) {

    companion object {
        // Re-query a small window before the last-seen cursor so cross-device clock
        // skew can't drop edits. Overlapping docs just upsert again — harmless.
        const val SKEW_BUFFER_MS = 5 * 60_000L          // 5 minutes
        // How often a full-collection pull runs to catch cross-device deletions.
        const val FULL_RECONCILE_INTERVAL_MS = 24 * 60 * 60_000L   // 24 hours
        // Skip the network call entirely if pulled this recently.
        const val TTL_MS = 15 * 60_000L                 // 15 minutes
    }

    private fun nowMs(): Long = System.currentTimeMillis()

    /**
     * The pull plan for an incremental-capable target.
     * fullReconcile=true → pull the whole collection. Otherwise pull only docs with
     * updatedAt > sinceMs.
     */
    data class PullPlan(val fullReconcile: Boolean, val sinceMs: Long?)

    suspend fun planFor(key: String): PullPlan {
        val marker = dao.get(key)
        val full = marker == null ||
            (nowMs() - marker.lastFullPullMs) > FULL_RECONCILE_INTERVAL_MS
        return if (full) {
            PullPlan(fullReconcile = true, sinceMs = null)
        } else {
            val since = (marker!!.lastSeenRemoteMs - SKEW_BUFFER_MS).coerceAtLeast(0L)
            PullPlan(fullReconcile = false, sinceMs = since)
        }
    }

    /**
     * TTL guard: true means we pulled (any kind) recently enough to skip the network call
     * entirely. Used to debounce a repeat pull — a manual refresh tap, or a periodic run
     * that happens to land shortly after another sync already completed.
     */
    suspend fun withinTtl(key: String): Boolean {
        val marker = dao.get(key) ?: return false
        return (nowMs() - marker.lastPullMs) < TTL_MS
    }

    /**
     * Persist the outcome of a pull.
     * @param maxRemoteMs the max remote timestamp observed among pulled docs (0 if none/unknown).
     * @param wasFull whether this was a full-collection reconcile.
     */
    suspend fun recordPull(key: String, maxRemoteMs: Long, wasFull: Boolean) {
        val existing = dao.get(key)
        val now = nowMs()
        dao.upsert(
            SyncMarker(
                key = key,
                lastSeenRemoteMs = maxOf(existing?.lastSeenRemoteMs ?: 0L, maxRemoteMs),
                lastPullMs = now,
                lastFullPullMs = if (wasFull) now else (existing?.lastFullPullMs ?: 0L)
            )
        )
    }
}
