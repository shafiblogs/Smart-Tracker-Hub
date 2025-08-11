package com.marsa.smarttrackerhub.data.helper

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