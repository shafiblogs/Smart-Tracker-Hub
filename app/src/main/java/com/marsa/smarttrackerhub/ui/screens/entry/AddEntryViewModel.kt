package com.marsa.smarttrackerhub.ui.screens.entry

import androidx.lifecycle.ViewModel

/**
 * Created by Muhammed Shafi on 22/06/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class AddEntryViewModel : ViewModel() {
//    private val _screenType = MutableStateFlow(ScreenType.Purchase)
//    private val screenType: StateFlow<ScreenType> = _screenType
//
//    private val _selectedDate = MutableStateFlow(LocalDate.now())
//    val selectedDate: StateFlow<String> = _selectedDate
//        .map { it.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
//        .stateIn(
//            viewModelScope,
//            SharingStarted.Eagerly,
//            LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
//        )
//
//    private val _categoryList = MutableStateFlow<List<Category>>(emptyList())
//    val categoryList: StateFlow<List<Category>> = _categoryList
//
//    private val _vendorList = MutableStateFlow<List<Vendor>>(emptyList())
//    val vendorList: StateFlow<List<Vendor>> = _vendorList
//
//    private val _formData = MutableStateFlow(AddEntryFormData())
//    val formData: StateFlow<AddEntryFormData> = _formData.asStateFlow()
//
//    private val _saveSuccess = MutableStateFlow(false)
//    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()
//
//    private val _isUpdate = MutableStateFlow(false)
//    val isUpdate: StateFlow<Boolean> = _isUpdate.asStateFlow()
//
//    val paymentTypes = mutableListOf(
//        "Select Payment Type", "Cash", "Credit Card", "Bank Transfer"
//    )
//
//    init {
//        val default = paymentTypes[0]
//        _formData.update { it.copy(paymentType = default) }
//    }
//
//    private lateinit var db: AppDatabase
//    private var pendingCategoryId: Int? = null
//    private var pendingVendorId: Int? = null
//
//    private fun initDatabase(context: Context) {
//        db = AppDatabase.getDatabase(context)
//        loadCategories()
//    }
//
//    private fun loadCategories() = viewModelScope.launch {
//        val categories = db.categoryDao().getCategoriesByType(screenType.value.name)
//        _categoryList.value = categories
//
//        // Apply pending category after loading
//        pendingCategoryId?.let { id ->
//            updateCategory(id)
//            pendingCategoryId = null
//        }
//    }
//
//    private fun loadVendors() = viewModelScope.launch {
//        val vendors = db.vendorDao().getVendorsByCategory(_formData.value.categoryId)
//        _vendorList.value = vendors
//
//        // Apply pending vendor after loading
//        pendingVendorId?.let { id ->
//            updateVendor(id)
//            pendingVendorId = null
//        }
//    }
//
//    fun updateCategory(categoryId: Int) {
//        val selectedCategory = _categoryList.value.find { it.id == categoryId }
//        _formData.update {
//            it.copy(
//                categoryId = categoryId,
//                category = selectedCategory?.name ?: "Select Category",
//                vendorId = 0,
//                vendor = "Select Vendor"
//            )
//        }
//        loadVendors()
//    }
//
//    fun updateVendor(vendorId: Int) {
//        val selectedVendor = _vendorList.value.find { it.id == vendorId }
//        _formData.update {
//            it.copy(
//                vendorId = vendorId,
//                vendor = selectedVendor?.name ?: "Select Category"
//            )
//        }
//    }
//
//    fun updateScreenType(type: ScreenType, context: Context, entryItem: EntryWithCategory?) {
//        _screenType.value = type
//        if (_screenType.value == ScreenType.Expense) {
//            paymentTypes.add("Not Applicable")
//        }
//        initDatabase(context)
//        _isUpdate.value = entryItem != null
//
//        if (entryItem != null) {
//            _formData.update {
//                it.copy(
//                    itemId = entryItem.entry.id,
//                    amount = entryItem.entry.amount,
//                    paymentType = entryItem.entry.paymentType
//                )
//            }
//            updateDate(entryItem.entry.date)
//
//            // Store IDs to apply after data loads
//            pendingCategoryId = entryItem.entry.categoryId
//            pendingVendorId = entryItem.entry.vendorId
//        }
//    }
//
//
//    fun updateAmount(amount: String) {
//        _formData.update { it.copy(amount = amount) }
//    }
//
//    fun updateDate(date: LocalDate) {
//        _selectedDate.value = date
//        _formData.update { it.copy(date = date) }
//    }
//
//    fun updatePaymentType(paymentType: String) {
//        _formData.update { it.copy(paymentType = paymentType) }
//    }
//
//    fun saveEntry(context: Context) = viewModelScope.launch {
//        val data = _formData.value
//        when {
//            data.amount.isBlank() -> {
//                Toast.makeText(context, "Amount cannot be empty", Toast.LENGTH_SHORT).show()
//                _saveSuccess.value = false
//                return@launch
//            }
//
//            data.categoryId == 0 -> {
//                Toast.makeText(context, "Please select a Category", Toast.LENGTH_SHORT).show()
//                _saveSuccess.value = false
//                return@launch
//            }
//
//            data.paymentType == "Select Payment Type" -> {
//                Toast.makeText(context, "Please select a Payment Type", Toast.LENGTH_SHORT).show()
//                _saveSuccess.value = false
//                return@launch
//            }
//
//            data.vendorId == 0 -> {
//                Toast.makeText(context, "Please select a Vendor", Toast.LENGTH_SHORT).show()
//                _saveSuccess.value = false
//                return@launch
//            }
//
//            else -> {
//                val repo = EntryRepository(
//                    AppDatabase.getDatabase(context).entryDao(),
//                    AppDatabase.getDatabase(context).summaryDao()
//                )
//
//                repo.insert(
//                    data,
//                    if (screenType.value == ScreenType.Purchase) EntryType.PURCHASE else EntryType.EXPENSE,
//                    isUpdate.value
//                )
//
//                _formData.value = AddEntryFormData() // Reset form
//                _saveSuccess.value = true
//            }
//        }
//    }
}
