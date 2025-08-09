package com.marsa.smarttrackerhub.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


/**
 * Created by Muhammed Shafi on 01/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun DateHeader(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        SmallTextField(
            value = date,
            fontWeight = FontWeight.SemiBold
        )
    }
}