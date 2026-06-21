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
 *  3. PUSHES again — flushes any local changes that were preserved during pull
 *     (e.g. shop status edits that failed to push before the pull ran)
 *
 * Pass [KEY_FORCE_RESYNC] = true as input data to mark all shops as unsynced
 * before the first push, so their current local values overwrite stale Firestore data.
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

    companion object {
        /** Set to true in WorkManager inputData to force-push all shop records, regardless of isSynced. */
        const val KEY_FORCE_RESYNC = "forceResync"
    }

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val syncRepo = FirebaseSyncRepository(db)
            val pullRepo = FirebasePullRepository(db)

            // Optional: mark all records unsynced so their current local values are (re)pushed.
            // Covers the whole investor domain too, so existing investments/shares/settlements
            // entered before sync existed are guaranteed to upload — no data left behind.
            val forceResync = inputData.getBoolean(KEY_FORCE_RESYNC, false)
            if (forceResync) {
                db.shopDao().markAllShopsUnsynced()
                db.investorDao().markAllInvestorsUnsynced()
                db.shopInvestorDao().markAllShopInvestorsUnsynced()
                db.investmentTransactionDao().markAllTransactionsUnsynced()
                db.yearEndSettlementDao().markAllSettlementsUnsynced()
                db.yearEndSettlementDao().markAllSettlementEntriesUnsynced()
                Log.d("SyncWorker", "Force resync: all shops + investor-domain records marked unsynced")
            }

            // 1. Push: upload all unsynced local records to Firestore
            syncRepo.syncAll()
            Log.d("SyncWorker", "Push (pass 1) completed successfully")

            // 2. Pull: download all Firestore records into Room (multi-device sync)
            //    pullShops() preserves local shopStatus when isSynced = false,
            //    so any locally-edited-but-not-yet-pushed records survive the pull.
            pullRepo.pullAll()
            Log.d("SyncWorker", "Pull completed successfully")

            // 3. Push again: flush any local changes preserved during pull back to Firestore
            syncRepo.syncAll()
            Log.d("SyncWorker", "Push (pass 2) completed successfully")

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed: ${e.message}")
            Result.retry()
        }
    }
}
