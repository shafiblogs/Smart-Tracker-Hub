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
        FirebaseApp.initializeApp(this)

        // Initialize Firebase App (Smart Tracker)
        val options = FirebaseOptions.Builder()
            .setApplicationId("com.marsa.smarttracker")
            .setApiKey("AIzaSyC2l2MAQEBsRLt9-X5JMyIrRrZ0Gz6vwbA")
            .setProjectId("smart-tracker-8012f")
            .setStorageBucket("smart-tracker-8012f.appspot.com")
            .build()

        val firebaseApp = FirebaseApp.initializeApp(applicationContext, options, "com.marsa.smarttracker")
        Log.d("Firebase", "Initialized app: ${firebaseApp.name}, is default: ${FirebaseApp.getApps(this).contains(firebaseApp)}")

        // Use Firebase Firestore
        //val firestore = FirebaseFirestore.getInstance(firebaseApp)

    }
}