package com.marsa.smarttrackerhub.utils

import androidx.compose.runtime.Composable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


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