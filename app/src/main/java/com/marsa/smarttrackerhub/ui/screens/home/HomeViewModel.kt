package com.marsa.smarttrackerhub.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.repository.SalesRepository
import com.marsa.smarttrackerhub.domain.SalesSummary
import com.marsa.smarttrackerhub.helper.getFormatedDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


/**
 * Created by Muhammed Shafi on 14/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _entries = MutableStateFlow<List<SalesSummary>>(emptyList())
    val entries: StateFlow<List<SalesSummary>> = _entries

    private val salesRepository: SalesRepository by lazy {
        SalesRepository(AppDatabase.getDatabase(application).salesDao())
    }

    init {
        viewModelScope.launch {
            salesRepository.allSalesFlow
                .map { salesList ->
                    salesList.map { saleEntity ->
                        val totalSale =
                            saleEntity.cashSale + saleEntity.cardSale + saleEntity.creditSale
                        val totalCashIn = saleEntity.cashSale + saleEntity.cashPayment

                        SalesSummary(
                            dateKey = saleEntity.date,
                            date = getFormatedDate(saleEntity.date),
                            totalSale = totalSale,
                            cashIn = totalCashIn,
                            cashSale = saleEntity.cashSale,
                            cardSale = saleEntity.cardSale,
                            creditSale = saleEntity.creditSale,
                            cashPayment = saleEntity.cashPayment,
                            cardPayment = saleEntity.cardPayment
                        )
                    }
                }
                .collect { summaryList ->
                    _entries.value = summaryList
                }
        }
    }
}