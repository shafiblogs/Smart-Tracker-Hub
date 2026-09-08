package com.marsa.smarttrackerhub

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.marsa.smarttrackerhub.data.worker.SyncWorker
import java.util.concurrent.TimeUnit


/**
 * Created by Muhammed Shafi on 08/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class SmartTrackerHubApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Default app (Hub project)
        FirebaseApp.initializeApp(this)

        // Eagerly sign in to Hub Firebase so shop/investor/employee sync can run immediately.
        FirebaseAuth.getInstance()
            .signInAnonymously()
            .addOnSuccessListener { Log.d("SmartTrackerHubApp", "Hub app signed in") }
            .addOnFailureListener { e -> Log.e("SmartTrackerHubApp", "Hub app sign-in failed: ${e.message}") }

        // SmartTracker Firebase — Firestore summaries (Sale/Purchase) + PDF Storage (Statement)
        // project: smart-tracker-8012f
        val trackerOptions = FirebaseOptions.Builder()
            .setApplicationId("1:979114972932:android:86e1d8fefb4d376a71a251")
            .setApiKey(BuildConfig.api_key)
            .setProjectId(BuildConfig.project_id)
            .setStorageBucket(BuildConfig.storage_bucket)
            .build()
        FirebaseApp.initializeApp(this, trackerOptions, "SmartTrackerApp")

        // Eagerly sign in to SmartTrackerApp Firebase so Sale/Purchase/Summary screens
        // can access Firestore immediately without waiting for their own ViewModel auth.
        FirebaseAuth.getInstance(FirebaseApp.getInstance("SmartTrackerApp"))
            .signInAnonymously()
            .addOnSuccessListener { Log.d("SmartTrackerHubApp", "SmartTrackerApp signed in") }
            .addOnFailureListener { e -> Log.e("SmartTrackerHubApp", "SmartTrackerApp sign-in failed: ${e.message}") }

        // AccountTracker Firebase — OPS PDF Storage (ops_uae / ops_kuwait)
        // project: accounts-tracker-16f93
        val accountOptions = FirebaseOptions.Builder()
            .setApplicationId("1:1061133708867:android:4213179a6575e6dc15aef6")
            .setApiKey(BuildConfig.api_key_account)
            .setProjectId(BuildConfig.project_id_account)
            .setStorageBucket(BuildConfig.storage_bucket_account)
            .build()
        FirebaseApp.initializeApp(this, accountOptions, "AccountTrackerApp")

        scheduleSyncWorker()
    }

    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Periodic sync — runs once daily while connected to push any isSynced=false records,
        // and (scope=ALL, the default) pull shops/employees/investors/transactions/settlements
        // too — this is what keeps notification-source data (shop licenses, employee visas)
        // fresh on a device where nobody happened to open the Shops/Employees screen.
        //
        // Policy is UPDATE, not KEEP: KEEP means enqueueUniquePeriodicWork is a no-op whenever a
        // worker already exists under this name, so on any device that already has the app
        // installed, a future change to this schedule would silently never take effect — the
        // on-device schedule stays pinned at whatever first ran on that device. UPDATE also
        // matters directly for cross-device sync: a change made on one device should reach every
        // other device within a bounded, predictable window, not "whenever that device happens
        // to reinstall."
        val periodicSync = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "firebase_sync_periodic",
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicSync
        )
        Log.d("SmartTrackerHubApp", "Daily SyncWorker scheduled")
    }
}