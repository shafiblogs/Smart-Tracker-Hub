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
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.domain.getStatementShopList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * Created by Muhammed Shafi on 31/05/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class StatementViewModel(firebaseApp: FirebaseApp) : ViewModel() {
    private val _shops = MutableStateFlow<List<ShopListDto>>(emptyList())
    val shops: StateFlow<List<ShopListDto>> = _shops

    private val _selectedShop = MutableStateFlow<ShopListDto?>(null)
    val selectedShop: StateFlow<ShopListDto?> = _selectedShop

    private val _expanded = MutableStateFlow(false)
    val expanded: StateFlow<Boolean> = _expanded

    fun setSelectedShop(shop: ShopListDto?) {
        _selectedShop.value = shop
    }

    fun setExpanded(value: Boolean) {
        _expanded.value = value
    }

    private val storage = FirebaseStorage.getInstance(firebaseApp)

    fun loadScreenData(userAccessCode: AccessCode) {
        _shops.value = getStatementShopList(userAccessCode)
        loadStatements()
    }

    private fun loadStatements() {
        viewModelScope.launch(Dispatchers.IO) {
            val shopFilesMap = _shops.value
                .filter { !it.shopId.isNullOrEmpty() && !it.folderPath.isNullOrEmpty() }
                .associate { shop ->
                    shop.shopId!! to fetchStatementFiles(shop.shopId, shop.folderPath!!)
                }
            updateShopsWithStatements(shopFilesMap)
        }
    }


    private fun fetchStatementFiles(
        shopId: String,
        folderUrl: String
    ): List<StatementFile> {
        return try {
            val folderRef = storage.getReferenceFromUrl(folderUrl)
            val listResult = Tasks.await(folderRef.listAll())

            listResult.items.map { fileRef ->
                val url = Tasks.await(fileRef.downloadUrl).toString()
                val month = parseMonthFromFilename(fileRef.name)
                StatementFile(month = month, url = url)
            }
        } catch (e: Exception) {
            Log.e("Exception", "Error loading statement files for shop $shopId from $folderUrl", e)
            emptyList()
        }
    }

    private fun updateShopsWithStatements(shopFilesMap: Map<String, List<StatementFile>>) {
        _shops.update { currentShops ->
            currentShops.map { shop ->
                shop.shopId?.let { shopId ->
                    val statementFiles = shopFilesMap[shopId] ?: emptyList()
                    shop.copy(statementFiles = statementFiles)
                } ?: shop
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
