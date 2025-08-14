package com.marsa.smarttrackerhub.data.repository


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
