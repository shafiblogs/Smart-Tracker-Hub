package com.marsa.smarttrackerhub.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel


/**
 * Created by Muhammed Shafi on 23/06/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class HomeScreenViewModel(application: Application) : AndroidViewModel(application) {

//    private val entryRepository: EntryRepository by lazy {
//        EntryRepository(
//            AppDatabase.getDatabase(application).entryDao(),
//            AppDatabase.getDatabase(application).summaryDao()
//        )
//    }
//
//    // List of sales records
//    private val _salesRecordsList = MutableStateFlow<List<EntryWithCategory>>(emptyList())
//    val salesRecordsList: StateFlow<List<EntryWithCategory>> = _salesRecordsList
//
//    // Reactive flow for balance model
//    private val _balanceModel = MutableStateFlow(BalanceModel(0.0, 0.0, 0.0))
//    val balanceModel: StateFlow<BalanceModel> = _balanceModel
//
//    init {
//        // Get sales records and balance model
//        getSalesRecords()
//        observeBalanceModel()
//    }
//
//    // Fetch sales records for the week
//    private fun getSalesRecords() = viewModelScope.launch {
//        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
//        val (start, end) = getDateRangeFor("This Week")
//        val startDateStr = start.format(formatter)
//        val endDateStr = end.format(formatter)
//
//        entryRepository.getEntries(
//            EntryType.PURCHASE,
//            startDateStr,
//            endDateStr
//        ).collectLatest { entries ->
//            _salesRecordsList.value = entries.take(3)
//        }
//    }
//
//    // Observe the reactive flow for balance model
//    private fun observeBalanceModel() = viewModelScope.launch {
//        val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
//        entryRepository.observeBalanceModel(currentMonth).collect { balance ->
//            _balanceModel.value = balance // Update balance when observed data changes
//        }
//    }
}
