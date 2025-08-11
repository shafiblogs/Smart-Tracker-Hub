package com.marsa.smarttrackerhub

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore


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
            .setApiKey("###")
            .setProjectId("##")
            .setStorageBucket("##")
            .build()
        FirebaseApp.initializeApp(this, trackerOptions, "SmartTrackerApp")
    }
}