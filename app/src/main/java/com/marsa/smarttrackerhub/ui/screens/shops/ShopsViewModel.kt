package com.marsa.smarttrackerhub.ui.screens.shops

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter


/**
 * Created by Muhammed Shafi on 31/05/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class ShopsViewModel(application: Application) : AndroidViewModel(application) {

    val entries = listOf(String)

    private val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    private val _files = MutableStateFlow<List<String>>(emptyList())
    val files: StateFlow<List<String>> = _files.asStateFlow()

    private val _statements = MutableStateFlow<List<Statement>>(emptyList())
    val statements: StateFlow<List<Statement>> = _statements.asStateFlow()

    private val _shops = MutableStateFlow<List<Shop>>(emptyList())
    val shops: StateFlow<List<Shop>> = _shops.asStateFlow()

    init {
        fetchShops()
    }



    private fun fetchShops() {
        FirebaseFirestore.getInstance()
            .collection("shops")
            .get()
            .addOnSuccessListener { result ->
                val shopList = result.documents.map { doc ->
                    Shop(
                        id = doc.id,
                        name = doc.getString("name") ?: doc.id  // fallback if name field is missing
                    )
                }
                _shops.value = shopList
            }
            .addOnFailureListener {
                Log.e("##Firestore", "Failed to fetch shops", it)
            }
    }

    private fun fetchStatements() {
        FirebaseFirestore.getInstance()
            .collection("statements")
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Statement::class.java)
                }
                _statements.value = items
            }
            .addOnFailureListener {
                Log.e("##Firestore", "Failed to fetch statements", it)
            }
    }

    private fun fetchUploadedFiles() {
        val storageRef = FirebaseStorage.getInstance().reference.child("tracker")

        storageRef.listAll()
            .addOnSuccessListener { result ->
                _files.value = result.items.map { it.name }
            }
            .addOnFailureListener {
                Log.e("ShopsViewModel", "Failed to list files", it)
            }
    }

}
