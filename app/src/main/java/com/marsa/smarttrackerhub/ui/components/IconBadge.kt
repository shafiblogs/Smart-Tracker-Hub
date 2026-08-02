package com.marsa.smarttrackerhub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * An icon inside a circular tinted background — the shared "status pill" treatment used across
 * SmartTracker & AccountsTracker (icon tinted with [color]; circle = [color] at 12% alpha).
 */
@Composable
fun IconBadge(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    circleSize: Dp = 32.dp,
    iconSize: Dp = 18.dp,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(circleSize)
            .background(color.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.size(iconSize)
        )
    }
}
