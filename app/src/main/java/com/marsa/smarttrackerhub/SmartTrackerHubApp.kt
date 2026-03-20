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