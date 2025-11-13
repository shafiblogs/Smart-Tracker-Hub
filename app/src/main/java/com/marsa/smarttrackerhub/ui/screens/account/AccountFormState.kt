package com.marsa.smarttrackerhub.ui.screens.account

import com.marsa.smarttrackerhub.domain.AccessCode


/**
 * Created by Muhammed Shafi on 18/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
data class AccountFormState(
    val accessCode: String = AccessCode.GUEST.code,
    val userName: String = "",
    val password: String = "",
    val confirmPassword: String = ""
)