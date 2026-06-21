package com.marsa.smarttrackerhub.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.io.File
import java.io.FileOutputStream

/**
 * Utility class for sharing chart and statistics as images
 */
object ShareUtil {

    /**
     * Captures a view as bitmap and shares it
     *
     * @param view The view to capture
     * @param context Application context
     * @param fileName Name for the saved image file
     * @param shareTitle Title for the share dialog
     */
    fun shareViewAsImage(
        view: View,
        context: Context,
        fileName: String = "sales_stats_${System.currentTimeMillis()}.png",
        shareTitle: String = "Share Sales Statistics"
    ) {
        try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            shareBitmap(context, bitmap, fileName, shareTitle)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Renders a composable OFF-SCREEN at a fixed [widthPx] and its full (wrap) content
     * height, then captures the whole thing to a PNG and shares it. Use this for content
     * that is taller than the visible area (e.g. a category bar chart with many rows) so
     * the shared image is never clipped — unlike [shareViewAsImage] which only captures the
     * on-screen, possibly-scrolled view.
     */
    fun shareComposableAsImage(
        activity: ComponentActivity,
        widthPx: Int,
        fileName: String = "chart_${System.currentTimeMillis()}.png",
        shareTitle: String = "Share",
        content: @Composable () -> Unit
    ) {
        try {
            val root = activity.window.decorView as ViewGroup
            val composeView = ComposeView(activity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                // Provide the owners Compose needs to run a real composition off-screen.
                setViewTreeLifecycleOwner(activity)
                setViewTreeViewModelStoreOwner(activity)
                setViewTreeSavedStateRegistryOwner(activity)
                // Keep it invisible so the user never sees the off-screen render.
                visibility = View.INVISIBLE
                setContent(content)
            }

            root.addView(
                composeView,
                ViewGroup.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
            )

            // Wait until the composition has laid out (non-zero height), then snapshot.
            composeView.doOnPreDraw {
                try {
                    val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
                    val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    composeView.measure(widthSpec, heightSpec)
                    composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

                    val bitmap = Bitmap.createBitmap(
                        composeView.measuredWidth.coerceAtLeast(1),
                        composeView.measuredHeight.coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    composeView.draw(Canvas(bitmap))
                    shareBitmap(activity, bitmap, fileName, shareTitle)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    root.removeView(composeView)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Internal ────────────────────────────────────────────────────────────────

    /** Saves [bitmap] to the cache dir and fires an ACTION_SEND chooser. */
    private fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String, shareTitle: String) {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()

        val file = File(cachePath, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, contentUri)
            type = "image/png"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, shareTitle))
    }
}
