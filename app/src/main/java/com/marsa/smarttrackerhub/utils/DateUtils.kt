package com.marsa.smarttrackerhub.utils


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