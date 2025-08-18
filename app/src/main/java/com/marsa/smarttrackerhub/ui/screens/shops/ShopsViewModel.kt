package com.marsa.smarttrackerhub.ui.screens.shops

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
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
class ShopsViewModel(private val firebaseApp: FirebaseApp) : ViewModel() {
    private val _shops = MutableStateFlow<List<Shop>>(emptyList())
    val shops: StateFlow<List<Shop>> = _shops

    private val trackerFireStore = FirebaseFirestore.getInstance(firebaseApp)
    private val storage = FirebaseStorage.getInstance(firebaseApp)

    init {
        loadShops()
        loadStatements()
    }

    private fun loadStatements() {
        viewModelScope.launch(Dispatchers.IO) {
            val folders = listOf(
                "gs://smart-tracker-8012f.firebasestorage.app/marsa/masfout",
                "gs://smart-tracker-8012f.firebasestorage.app/marsa/muzeira",
                "gs://smart-tracker-8012f.firebasestorage.app/wadi/muzeira"
            )

            val urlMap = mutableMapOf<String, String>() // Map<shopId, pdfUrl>

            for (folderUrl in folders) {
                val folderRef = storage.getReferenceFromUrl(folderUrl)
                try {
                    val listResult = Tasks.await(folderRef.listAll())
                    for (fileRef in listResult.items) {
                        val url = Tasks.await(fileRef.downloadUrl).toString()
                        if (folderUrl.contains("marsa/masfout")) {
                            urlMap["MARSA_102"] = url
                        } else if (folderUrl.contains("marsa/muzeira")) {
                            urlMap["MARSA_101"] = url
                        } else if (folderUrl.contains("wadi/muzeira")) {
                            urlMap["WADI_101"] = url
                        }
                    }
                } catch (e: Exception) {
                    Log.e("StatementsViewModel", "Error listing files in $folderUrl", e)
                }
            }

            // Update shops with PDF URLs
            val updatedShops = _shops.value.map { shop ->
                val pdfUrl = shop.shopId?.let { urlMap[it] }
                shop.copy(pdfUrl = pdfUrl ?: shop.pdfUrl)
            }

            _shops.value = updatedShops
        }
    }


    private fun loadShops() {
        trackerFireStore.collection("shops")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ShopsViewModel", "Error fetching shops", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _shops.value = snapshot.documents.mapNotNull {
                        it.toObject(Shop::class.java)?.copy(shopId = it.id)
                    }
                }
            }
    }
}

fun openPdf(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(url), "application/pdf")
        flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No PDF viewer found", Toast.LENGTH_SHORT).show()
    }
}
