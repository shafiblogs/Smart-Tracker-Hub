package com.marsa.smarttrackerhub.data.repository

import com.marsa.smarttrackerhub.data.dao.EntryDao
import com.marsa.smarttrackerhub.data.dao.SalesDao
import com.marsa.smarttrackerhub.data.dao.SummaryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine


/**
 * Created by Muhammed Shafi on 25/06/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class SummaryRepository(
    private val dao: SummaryDao, private val entryDao: EntryDao,
    private val saleDao: SalesDao
) {
    fun observeDatesForMonth(month: String): Flow<List<String>> {
        val entryFlow = entryDao.getEntryDatesFlow(month)
        val salesFlow = saleDao.getSalesDatesFlow(month)

        return combine(entryFlow, salesFlow) { entryDates, salesDates ->
            (entryDates + salesDates).distinct().sorted()
        }
    }

    // Get the cash balance of the previous month from the summary table
    suspend fun getCashBalanceByMonth(month: String): Double? {
        return dao.getCashBalanceByMonth(month)
    }
}