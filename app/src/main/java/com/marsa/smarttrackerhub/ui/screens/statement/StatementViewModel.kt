package com.marsa.smarttrackerhub.ui.screens.statement

import android.util.Log
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class StatementViewModel(
    firebaseSmartTracker: FirebaseApp,
    firebaseAccountTracker: FirebaseApp
) : ViewModel() {

    private val _shops = MutableStateFlow<List<ShopListDto>>(emptyList())
    val shops: StateFlow<List<ShopListDto>> = _shops

    private val _selectedShop = MutableStateFlow<ShopListDto?>(null)
    val selectedShop: StateFlow<ShopListDto?> = _selectedShop

    private val _statementFiles = MutableStateFlow<List<StatementFile>>(emptyList())
    val statementFiles: StateFlow<List<StatementFile>> = _statementFiles

    private val _isLoadingStatements = MutableStateFlow(false)
    val isLoadingStatements: StateFlow<Boolean> = _isLoadingStatements

    private val _expanded = MutableStateFlow(false)
    val expanded: StateFlow<Boolean> = _expanded

    private val storageSmartTracker = FirebaseStorage.getInstance(firebaseSmartTracker)
    private val storageAccountTracker = FirebaseStorage.getInstance(firebaseAccountTracker)

    // Cache to avoid reloading
    private val statementsCache = mutableMapOf<String, List<StatementFile>>()

    fun setSelectedShop(shop: ShopListDto?) {
        _selectedShop.value = shop
        _statementFiles.value = emptyList()

        shop?.let {
            if (!it.shopId.isNullOrEmpty() && !it.folderPath.isNullOrEmpty()) {
                loadStatementsForShop(it.shopId, it.folderPath)
            }
        }
    }

    fun setExpanded(value: Boolean) {
        _expanded.value = value
    }

    fun loadScreenData(userAccessCode: AccessCode) {
        _shops.value = getStatementShopList(userAccessCode)
        // Don't load statements here - wait for shop selection
    }

    private fun loadStatementsForShop(shopId: String, folderPath: String) {
        // Check cache first
        if (statementsCache.containsKey(shopId)) {
            _statementFiles.value = statementsCache[shopId] ?: emptyList()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingStatements.value = true

            try {
                val files = fetchStatementFiles(shopId, folderPath)
                statementsCache[shopId] = files
                _statementFiles.value = files
            } catch (e: Exception) {
                Log.e("StatementViewModel", "Error loading statements for $shopId", e)
                _statementFiles.value = emptyList()
            } finally {
                _isLoadingStatements.value = false
            }
        }
    }

    private fun fetchStatementFiles(
        shopId: String,
        folderUrl: String
    ): List<StatementFile> {
        return try {
            val storage =
                if (shopId.startsWith("ops_")) storageAccountTracker else storageSmartTracker

            val folderRef = storage.getReferenceFromUrl(folderUrl)
            val listResult = Tasks.await(folderRef.listAll())

            listResult.items
                .map { fileRef ->
                    val url = Tasks.await(fileRef.downloadUrl).toString()
                    val month = parseMonthFromFilename(fileRef.name)
                    StatementFile(month = month, url = url)
                }
                .sortedByDescending { parseMonthYear(it.month) }
        } catch (e: Exception) {
            Log.e(
                "StatementViewModel",
                "Error loading statement files for shop $shopId from $folderUrl",
                e
            )
            emptyList()
        }
    }

    private fun parseMonthYear(monthYear: String): Long {
        return try {
            val formatter = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
            formatter.parse(monthYear)?.time ?: 0L
        } catch (e: Exception) {
            Log.e("StatementViewModel", "Error parsing monthYear: $monthYear", e)
            0L
        }
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