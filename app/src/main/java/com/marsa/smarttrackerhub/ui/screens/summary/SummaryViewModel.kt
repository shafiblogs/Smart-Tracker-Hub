package com.marsa.smarttrackerhub.ui.screens.summary

import android.app.Application
import androidx.lifecycle.AndroidViewModel


/**
 * Created by Muhammed Shafi on 31/05/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class SummaryViewModel(application: Application) : AndroidViewModel(application) {

//    private val summaryRepository: SummaryRepository by lazy {
//        SummaryRepository(
//            AppDatabase.getDatabase(application).summaryDao(),
//            AppDatabase.getDatabase(application).entryDao(),
//            AppDatabase.getDatabase(application).salesDao()
//        )
//    }
//
//    private val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    val dailySummaries: StateFlow<List<DailySummary>> =
//        summaryRepository.observeDatesForMonth(currentMonth)
//            .mapLatest { dates ->
//                val openingBalance = summaryRepository.getCashBalanceByMonth(currentMonth) ?: 0.0
//
//                val summaries = mutableListOf<DailySummary>()
//                var balance = openingBalance
//
//                for (date in dates.sorted()) {
//                    val summary = summaryRepository.generateDailySummary(currentMonth,date, balance)
//                    summaries.add(summary)
//                    balance = summary.closingBalance
//                }
//
//                summaries
//            }
//            .stateIn(
//                scope = viewModelScope,
//                started = SharingStarted.WhileSubscribed(5000),
//                initialValue = emptyList()
//            )
}
