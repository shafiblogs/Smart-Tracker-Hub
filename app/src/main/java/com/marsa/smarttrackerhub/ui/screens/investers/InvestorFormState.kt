package com.marsa.smarttrackerhub.ui.screens.investers


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
data class InvestorFormState(
    val investorId: Int? = null,
    val investorName: String = "",
    val investorEmail: String = "",
    val investorPhone: String = "",
    val nameError: String? = null,
    val phoneError: String? = null
)