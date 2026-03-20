package com.marsa.smarttrackerhub.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.repository.FirebaseSyncRepository

/**
 * WorkManager worker that retries all unsynced Room records → Firestore.
 *
 * Scheduled as a daily [PeriodicWorkRequest] (network required).
 * Also triggered manually via the Sync Now button.
 * Only pushes records where isSynced = false — skips Firebase entirely if nothing is pending.
 *
 * Created by Muhammed Shafi on 13/03/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            FirebaseSyncRepository(db).syncAll()
            Log.d("SyncWorker", "Sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed: ${e.message}")
            Result.retry()
        }
    }
}
