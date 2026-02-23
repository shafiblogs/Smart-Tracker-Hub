package com.marsa.smarttrackerhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.marsa.smarttracker.ui.theme.SmartTrackerTheme
import com.marsa.smarttrackerhub.ui.navigation.SmartTrackerNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch back to the real app theme immediately so the OS system
        // splash screen (Android 12+) is dismissed before Compose renders.
        // This prevents the double-splash: OS icon splash → our Compose splash.
        setTheme(R.style.Theme_SmartTrackerHub)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartTrackerTheme {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        SmartTrackerNavHost(navController = navController)
                    }
                }
            }
        }
    }
}