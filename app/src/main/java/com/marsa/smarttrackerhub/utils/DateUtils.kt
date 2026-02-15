package com.marsa.smarttrackerhub.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.marsa.smarttrackerhub.domain.ExpiryStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


/**
 * Created by Muhammed Shafi on 14/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

/**
 * Gets short month name from monthYear string
 */

fun String.getShortMonthName(): String {
    return try {
        val parts = this.split(" - ")
        if (parts.isNotEmpty()) {
            val month = parts[0].trim()
            month.take(3) // First 3 letters
        } else {
            this
        }
    } catch (e: Exception) {
        this
    }
}

fun Long.formatTimestamp(): String {
    if (this == 0L) return "Never"
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return dateFormat.format(Date(this))
}

fun Long.getExpiryStatus(): ExpiryStatus {
    val currentTime = System.currentTimeMillis()
    val diffInMillis = this - currentTime
    val daysUntilExpiry = TimeUnit.MILLISECONDS.toDays(diffInMillis)

    return when {
        daysUntilExpiry < 0 -> ExpiryStatus("Expired", Color(0xFFD32F2F)) // Red
        daysUntilExpiry <= 60 -> ExpiryStatus("Near Expiry", Color(0xFFFF9800)) // Orange
        else -> ExpiryStatus("Active", Color(0xFF4CAF50)) // Green
    }
}