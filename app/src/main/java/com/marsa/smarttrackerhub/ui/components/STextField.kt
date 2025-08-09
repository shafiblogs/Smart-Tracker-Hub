package com.marsa.smarttrackerhub.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.marsa.smarttracker.ui.theme.sTypography


/**
 * Created by Muhammed Shafi on 14/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun SmallTextField(
    value: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        value,
        color = textColor,
        style = sTypography.bodySmall.copy(fontWeight = fontWeight),
        modifier = modifier
    )
}

@Composable
fun CommonTextField(
    value: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = sTypography.bodyMedium.copy(fontWeight = FontWeight.Medium)
) {
    Text(
        value,
        color = textColor,
        style = style,
        modifier = modifier
    )
}