package com.marsa.smarttrackerhub

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
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

        // Initialize Firebase App (Smart Tracker)
        val trackerOptions = FirebaseOptions.Builder()
            .setApplicationId("com.marsa.smarttracker")
            .setApiKey(BuildConfig.api_key)
            .setProjectId(BuildConfig.project_id)
            .setStorageBucket(BuildConfig.storage_bucket)
            .build()
        FirebaseApp.initializeApp(this, trackerOptions, "SmartTrackerApp")

        // Initialize Firebase App (Smart Tracker)
        val accountOptions = FirebaseOptions.Builder()
            .setApplicationId("com.marsa.accountstracker")
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

        // Periodic sync — runs once daily while connected to push any isSynced=false records
        val periodicSync = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "firebase_sync_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSync
        )

        // Immediate one-time sync on app start — flushes any records that missed the last window
        val immediateSync = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueue(immediateSync)
        Log.d("SmartTrackerHubApp", "SyncWorker scheduled")
    }
}