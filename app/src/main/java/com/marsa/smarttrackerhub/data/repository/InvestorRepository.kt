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

    suspend fun getInvestorById(id: Int): InvestorInfo? = investorDao.getInvestorById(id)

    suspend fun insertInvestor(investor: InvestorInfo) =
        investorDao.insertInvestor(investor.copy(updatedAt = System.currentTimeMillis()))

    suspend fun updateInvestor(investor: InvestorInfo) =
        investorDao.updateInvestor(investor.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteInvestor(investor: InvestorInfo) = investorDao.deleteInvestor(investor)

    /** Returns true if [investorId] is already used by another investor (excludes current record on edits). */
    suspend fun isInvestorIdTaken(investorId: String, excludeId: Int = 0): Boolean =
        investorDao.countByInvestorId(investorId, excludeId) > 0
}