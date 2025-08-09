package com.marsa.smarttrackerhub.ui.screens.category

/**
 * Created by Muhammed Shafi on 20/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
import androidx.lifecycle.ViewModel

class CategoryViewModel : ViewModel() {

//    private val _categories = MutableStateFlow<List<Category>>(emptyList())
//    val categories: StateFlow<List<Category>> = _categories.asStateFlow()
//
//    private val _selectedTrackFilter = MutableStateFlow(ScreenType.Purchase.name)
//    val selectedTrackFilter: StateFlow<String> = _selectedTrackFilter
//
//    private lateinit var db: AppDatabase
//
//    fun initDatabase(context: Context) {
//        db = AppDatabase.getDatabase(context)
//        loadCategories()
//    }
//
//    private fun loadCategories() = viewModelScope.launch {
//        _categories.value = db.categoryDao().getCategoriesByType(_selectedTrackFilter.value)
//    }
//
//    fun updateFilter(value: String) = viewModelScope.launch {
//        _selectedTrackFilter.value = value
//        loadCategories()
//    }
//
//    fun updateCategory(
//        id: Int,
//        isEdit: Boolean,
//        name: String,
//        description: String?,
//        screenType: ScreenType?,
//        onSuccess: () -> Unit = {},
//        onFail: (String) -> Unit = {}
//    ) {
//        viewModelScope.launch {
//            if (name.isBlank()) {
//                onFail("Category name cannot be empty")
//                return@launch
//            }
//
//            if (screenType == null) {
//                onFail("Please select a category type")
//                return@launch
//            }
//
//            try {
//                val updatedCategory = Category(
//                    id = id,
//                    name = name.trim(),
//                    description = description?.takeIf { it.isNotBlank() },
//                    screenType = screenType.name
//                )
//                if (isEdit) {
//                    db.categoryDao().updateCategory(updatedCategory)
//                } else {
//                    db.categoryDao().insertCategory(updatedCategory)
//                }
//                loadCategories()
//                onSuccess()
//            } catch (e: Exception) {
//                onFail("Failed to update category: ${e.localizedMessage}")
//            }
//        }
//    }
//
//
//    fun deleteCategory(categoryId: Int, onFail: () -> Unit) {
//        viewModelScope.launch {
//            val entryCount = db.entryDao().getEntryCountByCategory(categoryId)
//            val vendorCount = db.vendorDao().getVendorCountByCategory(categoryId)
//            if (entryCount == 0 && vendorCount == 0) {
//                db.categoryDao().deleteCategoryById(categoryId)
//                loadCategories()
//            } else {
//                onFail()
//            }
//        }
//    }
}
