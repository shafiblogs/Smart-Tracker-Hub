package com.marsa.smarttrackerhub.ui.screens.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Utility class for sharing chart as image
 */
object ChartShareUtil {
    
    /**
     * Captures a view as bitmap and shares it
     * 
     * @param view The view to capture (typically the chart view)
     * @param context Application context
     * @param fileName Name for the saved image file
     */
    fun shareChartImage(
        view: View,
        context: Context,
        fileName: String = "sales_chart_${System.currentTimeMillis()}.png"
    ) {
        try {
            // Create bitmap from view
            val bitmap = Bitmap.createBitmap(
                view.width,
                view.height,
                Bitmap.Config.ARGB_8888
            )
            
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            
            // Save bitmap to cache directory
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            
            val file = File(cachePath, fileName)
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            
            // Get URI for the file using FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            // Create share intent
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Start share dialog
            context.startActivity(Intent.createChooser(shareIntent, "Share Sales Chart"))
            
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle error - you might want to show a toast
        }
    }
    
    /**
     * Alternative: Captures chart canvas and shares
     * Use this if you want to capture just the chart without surrounding UI
     */
    fun shareChartCanvas(
        chartBitmap: Bitmap,
        context: Context,
        fileName: String = "sales_chart_${System.currentTimeMillis()}.png"
    ) {
        try {
            // Save bitmap to cache directory
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            
            val file = File(cachePath, fileName)
            val fileOutputStream = FileOutputStream(file)
            chartBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            
            // Get URI for the file using FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            // Create share intent
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Start share dialog
            context.startActivity(Intent.createChooser(shareIntent, "Share Sales Chart"))
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
