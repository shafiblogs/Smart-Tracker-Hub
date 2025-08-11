package com.marsa.smarttrackerhub.data.repository

import android.content.Context
import com.marsa.smarttrackerhub.data.entity.UserAccount
import com.marsa.smarttrackerhub.data.AppDatabase


/**
 * Created by Muhammed Shafi on 07/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

class StatementRepository(private val context: Context) {
    private val db: AppDatabase by lazy { AppDatabase.getDatabase(context) }
    private val entryDao = db.entryDao()
    private val summaryDao = db.summaryDao()
    private val salesDao = db.salesDao()
    private val userAccountDao = db.userAccountDao()

    private val summaryRepository = SummaryRepository(summaryDao, entryDao, salesDao)

    suspend fun getLatestMonthName(): String {
        return summaryDao.getLatestMonthName()
    }

    suspend fun getAccount(): UserAccount? {
        return UserAccountRepository(userAccountDao).getFirstAccount()
    }

    suspend fun getMonthNames(): List<String> {
        return summaryDao.getAllMonthNames()
    }
}
