package com.marsa.smarttrackerhub.utils

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marsa.smarttracker.ui.theme.SmartTrackerTheme

/**
 * Renders [content] off-screen (themed) at [widthPx] and shares it as a PNG. Shared by the
 * Sales, Account, and Home detail cards so the off-screen-render-and-share pattern lives in
 * one place instead of being copy-pasted per screen.
 */
fun shareCard(
    context: Context,
    widthPx: Int,
    fileName: String,
    shareTitle: String,
    content: @Composable () -> Unit
) {
    val activity = context as? ComponentActivity ?: return
    ShareUtil.shareComposableAsImage(
        activity = activity,
        widthPx = widthPx,
        fileName = fileName,
        shareTitle = shareTitle
    ) {
        SmartTrackerTheme {
            Surface {
                Box(modifier = Modifier.padding(16.dp)) { content() }
            }
        }
    }
}
