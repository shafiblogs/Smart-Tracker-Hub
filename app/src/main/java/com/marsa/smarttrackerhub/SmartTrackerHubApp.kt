package com.marsa.smarttrackerhub

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions


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
    }
}