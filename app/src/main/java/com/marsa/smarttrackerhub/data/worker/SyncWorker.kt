package com.marsa.smarttrackerhub.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.repository.FirebasePullRepository
import com.marsa.smarttrackerhub.data.repository.FirebaseSyncRepository

/**
 * WorkManager worker that:
 *  1. PUSHES all unsynced Room records → Firestore (existing behaviour)
 *  2. PULLS all Firestore records → Room (new — enables multi-device sync)
 *
 * Push is skipped entirely if nothing is pending (no auth, no network).
 * Pull always runs so Device B receives data entered on Device A.
 *
 * Scheduled as a daily [PeriodicWorkRequest] (network required).
 * Also triggered manually via the Sync Now button.
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

            // 1. Push: upload all unsynced local records to Firestore
            FirebaseSyncRepository(db).syncAll()
            Log.d("SyncWorker", "Push completed successfully")

            // 2. Pull: download all Firestore records into Room (multi-device sync)
            FirebasePullRepository(db).pullAll()
            Log.d("SyncWorker", "Pull completed successfully")

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed: ${e.message}")
            Result.retry()
        }
    }
}
