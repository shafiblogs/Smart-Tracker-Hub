package com.marsa.smarttrackerhub.helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.marsa.smarttrackerhub.R
import com.marsa.smarttrackerhub.data.entity.UserAccount
import com.marsa.smarttrackerhub.ui.screens.summary.DateRange
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters


/**
 * Created by Muhammed Shafi on 01/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

fun getDateRangeFor(filter: String): DateRange {
    val today = LocalDate.now()

    return when (filter) {
        "Today" -> {
            DateRange(today, today)
        }

        "This Week" -> {
            val startOfWeek = today.with(DayOfWeek.MONDAY)
            val endOfWeek = today.with(DayOfWeek.SUNDAY)
            DateRange(startOfWeek, endOfWeek)
        }

        "Last Week" -> {
            val lastWeekStart = today.minusWeeks(1).with(DayOfWeek.MONDAY)
            val lastWeekEnd = today.minusWeeks(1).with(DayOfWeek.SUNDAY)
            DateRange(lastWeekStart, lastWeekEnd)
        }

        "This Month" -> {
            val startOfMonth = today.withDayOfMonth(1)
            val endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())
            DateRange(startOfMonth, endOfMonth)
        }

        else -> {
            DateRange(today, today) // fallback: today
        }
    }
}

fun getDateRangeFromMonthName(monthName: String): DateRange {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
    val yearMonth = YearMonth.parse(monthName, formatter)

    val startOfMonth: LocalDate = yearMonth.atDay(1)
    val endOfMonth: LocalDate = yearMonth.atEndOfMonth()
    return DateRange(startOfMonth, endOfMonth)
}

fun getUpdatedMonthName(monthName: String): String {
    val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    val outputFormatter = DateTimeFormatter.ofPattern("MMMM - yyyy")

    val yearMonth = YearMonth.parse(monthName, inputFormatter)
    return yearMonth.format(outputFormatter)
}

fun getFormatedDate(dateString: String): String {
    val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return try {
        LocalDate.parse(dateString, inputFormatter).format(outputFormatter)
    } catch (e: Exception) {
        dateString
    }
}

fun Context.showNotification(title: String, message: String) {
    val channelId = "tracker_channel"
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channel = NotificationChannel(
        channelId,
        "Tracker Notifications",
        NotificationManager.IMPORTANCE_HIGH
    )
    notificationManager.createNotificationChannel(channel)

    val notification = NotificationCompat.Builder(this, channelId)
        .setContentTitle(title)
        .setContentText(message)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    notificationManager.notify(1, notification)
}

fun isAdminUser(account: UserAccount?): Boolean {
    if (account == null) return false
    return account.userRole == "admin"
}

