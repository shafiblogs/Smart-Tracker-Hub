package com.marsa.smarttrackerhub.data.repository

import com.marsa.smarttrackerhub.data.dao.SalesDao
import com.marsa.smarttrackerhub.data.entity.SaleEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter


/**
 * Created by Muhammed Shafi on 30/06/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class SalesRepository(private val dao: SalesDao) {
//    suspend fun insert(formData: SaleEntryFormData) {
//        val salesEntry = SaleEntity(
//            date = formData.date.toString(),
//            cashSale = formData.cashSale?.toDoubleOrNull() ?: 0.0,
//            cardSale = formData.cardSale?.toDoubleOrNull() ?: 0.0,
//            creditSale = formData.creditSale?.toDoubleOrNull() ?: 0.0,
//            cashPayment = formData.cashPayment?.toDoubleOrNull() ?: 0.0,
//            cardPayment = formData.cardPayment?.toDoubleOrNull() ?: 0.0
//        )
//        dao.insert(salesEntry)
//    }

    private val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val allSalesFlow: Flow<List<SaleEntity>> = dao.getAllSalesFlow(currentMonth)

    suspend fun getSalesByDate(date: String) = dao.getSalesByDate(date)

    suspend fun deleteEntryByDate(date: String) {
        dao.deleteEntryByDate(date)
    }
}