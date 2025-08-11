package com.marsa.smarttrackerhub.data.repository

import com.marsa.smarttrackerhub.data.dao.EntryDao
import com.marsa.smarttrackerhub.data.dao.SummaryDao
import com.marsa.smarttrackerhub.data.entity.SummaryEntity
import com.marsa.smarttracker.ui.screens.home.BalanceModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine


/**
 * Created by Muhammed Shafi on 22/06/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

class EntryRepository(private val dao: EntryDao, private val summaryDao: SummaryDao) {

    suspend fun deleteEntryById(entryId: Int) {
        dao.deleteEntryById(entryId = entryId)
    }
}