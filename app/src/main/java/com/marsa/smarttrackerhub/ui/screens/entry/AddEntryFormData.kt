package com.marsa.smarttrackerhub.ui.screens.entry

import java.time.LocalDate
import java.time.format.DateTimeFormatter


/**
 * Created by Muhammed Shafi on 22/06/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
data class AddEntryFormData(
    val itemId: Int = 0,
    val amount: String = "",
    val date: LocalDate = LocalDate.now(),
    val categoryId: Int = 0,
    val vendorId: Int = 0,
    val category: String = "Select Category",
    val vendor: String = "Select Vendor",
    val paymentType: String = "Select Payment Type"
)