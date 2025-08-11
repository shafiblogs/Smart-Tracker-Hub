package com.marsa.smarttrackerhub.data.helper

import androidx.room.TypeConverter
import java.time.LocalDate


/**
 * Created by Muhammed Shafi on 22/06/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate): String = value.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)
}