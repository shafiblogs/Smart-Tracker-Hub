package com.marsa.smarttrackerhub.utils

import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar

/**
 * Created by Muhammed Shafi on 15/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

object HijriDateUtils {

    // Hijri month names in Arabic
    private val hijriMonthNames = arrayOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الثاني", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    /**
     * Converts Gregorian date to Hijri format with day and month only
     * @param gregorianMillis Gregorian date in milliseconds
     * @return Formatted Hijri date (e.g., "12 رمضان")
     */
    fun getHijriDateDayMonth(gregorianMillis: Long): String {
        val ummalquraCalendar = UmmalquraCalendar()
        ummalquraCalendar.timeInMillis = gregorianMillis
        val day = ummalquraCalendar.get(UmmalquraCalendar.DAY_OF_MONTH)
        val monthIndex = ummalquraCalendar.get(UmmalquraCalendar.MONTH)
        return "$day ${hijriMonthNames[monthIndex]}"
    }

    /**
     * Converts Gregorian date to Hijri format with full date
     * @param gregorianMillis Gregorian date in milliseconds
     * @return Formatted Hijri date (e.g., "12/9/1447")
     */
    fun getHijriDateFull(gregorianMillis: Long): String {
        val ummalquraCalendar = UmmalquraCalendar()
        ummalquraCalendar.timeInMillis = gregorianMillis
        val day = ummalquraCalendar.get(UmmalquraCalendar.DAY_OF_MONTH)
        val month = ummalquraCalendar.get(UmmalquraCalendar.MONTH) + 1
        val year = ummalquraCalendar.get(UmmalquraCalendar.YEAR)
        return "$day/$month/$year"
    }

    /**
     * Converts Gregorian date to Hijri format with day, month name, and year
     * @param gregorianMillis Gregorian date in milliseconds
     * @return Formatted Hijri date (e.g., "12 رمضان 1447")
     */
    fun getHijriDateFullWithMonthName(gregorianMillis: Long): String {
        val ummalquraCalendar = UmmalquraCalendar()
        ummalquraCalendar.timeInMillis = gregorianMillis
        val day = ummalquraCalendar.get(UmmalquraCalendar.DAY_OF_MONTH)
        val monthIndex = ummalquraCalendar.get(UmmalquraCalendar.MONTH)
        val year = ummalquraCalendar.get(UmmalquraCalendar.YEAR)
        return "$day ${hijriMonthNames[monthIndex]} $year"
    }
}