package com.marsa.smarttrackerhub.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.marsa.smarttracker.ui.theme.sTypography
import com.marsa.smarttrackerhub.R
import com.marsa.smarttrackerhub.ui.components.CommonTextField
import kotlinx.coroutines.delay


/**
 * Created by Muhammed Shafi on 22/06/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onTimeout()
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CommonTextField(
            stringResource(R.string.app_name),
            style = sTypography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 32.sp)
        )
    }
}