package com.marsa.smarttrackerhub.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.repository.FirebasePullRepository
import com.marsa.smarttrackerhub.data.repository.FirebaseSyncRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Standardised sync worker. Two knobs control what it does:
 *
 *  - [KEY_SCOPE]: which data to sync —
 *      [SCOPE_SHOPS] (shops only), [SCOPE_EMPLOYEES] (employees only),
 *      [SCOPE_INVESTORS] (whole investor domain: investors + links + transactions +
 *      settlements + entries), or [SCOPE_ALL] (everything). Default = ALL.
 *  - [KEY_PUSH_ONLY]: if true, only PUSH local changes up (never pull). Used by the
 *      left-drawer "Sync Now" as a pure backup that can't alter local data.
 *
 * Flow: push(scope) → (if not push-only) pull(scope) → push(scope) again to flush.
 * Pull is additive-only, so it can never delete/overwrite local rows.
 *
 * Daily [PeriodicWorkRequest] runs with the defaults (scope=ALL, two-way).
 *
 * Created by Muhammed Shafi on 13/03/2026.
 * Moro Hub
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SCOPE = "scope"
        const val KEY_PUSH_ONLY = "pushOnly"

        const val SCOPE_ALL = "all"
        const val SCOPE_SHOPS = "shops"
        const val SCOPE_EMPLOYEES = "employees"
        const val SCOPE_INVESTORS = "investors"

        /** Process-wide lock so the daily periodic sync and any manual sync never run
         *  concurrently — prevents two pulls both passing the existence check and
         *  double-inserting the same Firestore document. */
        private val syncMutex = Mutex()
    }

    override suspend fun doWork(): Result = syncMutex.withLock {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val sync = FirebaseSyncRepository(db)
            val pull = FirebasePullRepository(db)

            val scope = inputData.getString(KEY_SCOPE) ?: SCOPE_ALL
            val pushOnly = inputData.getBoolean(KEY_PUSH_ONLY, false)

            suspend fun push() = when (scope) {
                SCOPE_SHOPS     -> sync.pushShops()
                SCOPE_EMPLOYEES -> sync.pushEmployees()
                SCOPE_INVESTORS -> sync.pushInvestorDomain()
                else            -> sync.syncAll()
            }
            suspend fun pull() = when (scope) {
                SCOPE_SHOPS     -> pull.pullShopsScoped()
                SCOPE_EMPLOYEES -> pull.pullEmployeesScoped()
                SCOPE_INVESTORS -> pull.pullInvestorDomain()
                else            -> pull.pullAll()
            }

            // 1. Push local changes for this scope, plus any pending deletion tombstones.
            push()
            sync.pushDeletions()
            Log.d("SyncWorker", "Push completed (scope=$scope, pushOnly=$pushOnly)")

            if (!pushOnly) {
                // 2. Apply remote deletions, then pull this scope (additive/newest-wins).
                pull.pullDeletions()
                pull()
                Log.d("SyncWorker", "Pull completed (scope=$scope)")
                // 3. Push again to flush anything resolved during pull.
                push()
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed: ${e.message}")
            Result.retry()
        }
    }
}
