package com.marsa.smarttrackerhub.data.repository

import com.marsa.smarttrackerhub.data.dao.InvestmentTransactionDao
import com.marsa.smarttrackerhub.data.entity.InvestmentTransaction
import com.marsa.smarttrackerhub.domain.PhaseTransactionDetail
import kotlinx.coroutines.flow.Flow

/**
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class InvestmentTransactionRepository(private val dao: InvestmentTransactionDao) {

    fun getTransactionsForShopInvestor(shopInvestorId: Int): Flow<List<InvestmentTransaction>> =
        dao.getTransactionsForShopInvestor(shopInvestorId)

    fun getTransactionsForShop(shopId: Int): Flow<List<PhaseTransactionDetail>> =
        dao.getTransactionsForShop(shopId)

    suspend fun getTotalPaidByInvestorForShop(shopId: Int, investorId: Int): Double =
        dao.getTotalPaidByInvestorForShop(shopId, investorId)

    suspend fun getTotalPaidForShop(shopId: Int): Double =
        dao.getTotalPaidForShop(shopId)

    suspend fun getTotalPaidByShopInvestor(shopInvestorId: Int): Double =
        dao.getTotalPaidByShopInvestor(shopInvestorId)

    suspend fun getTransactionsForShopSince(shopId: Int, fromDate: Long): List<PhaseTransactionDetail> =
        dao.getTransactionsForShopSince(shopId, fromDate)

    suspend fun insertTransaction(transaction: InvestmentTransaction): Long =
        dao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: InvestmentTransaction) =
        dao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: InvestmentTransaction) =
        dao.deleteTransaction(transaction)

    suspend fun deleteTransactionById(id: Int) =
        dao.deleteTransactionById(id)
}
