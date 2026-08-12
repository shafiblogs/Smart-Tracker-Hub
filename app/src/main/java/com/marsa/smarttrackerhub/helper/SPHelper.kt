package com.marsa.smarttrackerhub.helper

import android.content.Context
import androidx.core.content.edit


/**
 * Created by Muhammed Shafi on 06/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

fun saveTokenToPreferences(context: Context, token: String) {
    val sharedPreferences = context.getSharedPreferences("smart_tracker_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit { putString("fcm_token", token) }
}

fun getSavedTokenFromPreferences(context: Context): String? {
    val sharedPreferences = context.getSharedPreferences("smart_tracker_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getString("fcm_token", null)
}

fun saveShopIdToPreferences(context: Context, shopId: String) {
    val sharedPreferences = context.getSharedPreferences("smart_tracker_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit { putString("shop_id", shopId) }
}

fun getShopIdFromPreferences(context: Context): String? {
    val sharedPreferences = context.getSharedPreferences("smart_tracker_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getString("shop_id", null)
}

/** Wall-clock time of the last Home-screen auto-refresh hit against Firestore, or 0 if none yet. */
fun saveLastHomeAutoSyncTime(context: Context, timestamp: Long) {
    val sharedPreferences = context.getSharedPreferences("smart_tracker_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit { putLong("last_home_auto_sync", timestamp) }
}

fun getLastHomeAutoSyncTime(context: Context): Long {
    val sharedPreferences = context.getSharedPreferences("smart_tracker_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getLong("last_home_auto_sync", 0L)
}