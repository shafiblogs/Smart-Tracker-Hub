package com.marsa.smarttrackerhub.ui.screens.statement

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


/**
 * Created by Muhammed Shafi on 31/05/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class StatementViewModel(firebaseApp: FirebaseApp) : ViewModel() {
    private val _shops = MutableStateFlow<List<StatementDto>>(emptyList())
    val shops: StateFlow<List<StatementDto>> = _shops

    private val storage = FirebaseStorage.getInstance(firebaseApp)

    private val hardcodedShops = listOf(
        StatementDto(name = "Al Marsa Masfout", address = "Masfout, UAE", shopId = "MARSA_102"),
        StatementDto(name = "Al Marsa Muzeira", address = "Muzeira, UAE", shopId = "MARSA_101"),
        StatementDto(name = "AL Wadi Muzeira", address = "Muzeira, UAE", shopId = "WADI_101")
    )

    fun loadScreenData() {
        _shops.value = hardcodedShops
        loadStatements()
    }

    private fun loadStatements() {
        viewModelScope.launch(Dispatchers.IO) {
            val folders = mapOf(
                "MARSA_102" to "gs://smart-tracker-8012f.firebasestorage.app/marsa/masfout",
                "MARSA_101" to "gs://smart-tracker-8012f.firebasestorage.app/marsa/muzeira",
                "WADI_101" to "gs://smart-tracker-8012f.firebasestorage.app/wadi/muzeira"
            )

            val shopFilesMap = mutableMapOf<String, List<StatementFile>>()

            for ((shopId, folderUrl) in folders) {
                val folderRef = storage.getReferenceFromUrl(folderUrl)
                try {
                    val listResult = Tasks.await(folderRef.listAll())
                    val files = listResult.items.map { fileRef ->
                        val url = Tasks.await(fileRef.downloadUrl).toString()
                        val month = parseMonthFromFilename(fileRef.name)
                        StatementFile(month = month, url = url)
                    }
                    shopFilesMap[shopId] = files
                } catch (e: Exception) {
                    Log.e("ShopsViewModel", "Error listing files in $folderUrl", e)
                    shopFilesMap[shopId] = emptyList()
                }
            }

            _shops.value = hardcodedShops.map { shop ->
                val files = shop.shopId?.let { shopFilesMap[it] } ?: emptyList()
                shop.copy(statementFiles = files)
            }
        }
    }
}

fun openPdf(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(url.toUri(), "application/pdf")
        flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No PDF viewer found", Toast.LENGTH_SHORT).show()
    }
}

private fun parseMonthFromFilename(filename: String): String {
    return try {
        val regex = "([A-Za-z]+)\\s*-\\s*(\\d{4})".toRegex()
        val match = regex.find(filename)
        if (match != null) {
            val month = match.groupValues[1]
            val year = match.groupValues[2]
            "$month $year"
        } else {
            "Statement"
        }
    } catch (e: Exception) {
        "Statement"
    }
}
