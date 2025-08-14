package com.marsa.smarttrackerhub.data.repository

import com.marsa.smarttrackerhub.data.dao.InvestorDao
import com.marsa.smarttrackerhub.data.entity.InvestorInfo
import kotlinx.coroutines.flow.Flow


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class InvestorRepository(private val investorDao: InvestorDao) {

    fun getAllInvestors(): Flow<List<InvestorInfo>> = investorDao.getAllInvestors()

    suspend fun insertInvestor(investor: InvestorInfo) {
        investorDao.insertInvestor(investor)
    }

    suspend fun deleteInvestor(investor: InvestorInfo) {
        investorDao.deleteInvestor(investor)
    }
}
