package com.marsa.smarttrackerhub.data.repository

import com.marsa.smarttrackerhub.data.dao.SettlementEntryWithName
import com.marsa.smarttrackerhub.data.dao.YearEndSettlementDao
import com.marsa.smarttrackerhub.data.entity.SettlementEntry
import com.marsa.smarttrackerhub.data.entity.YearEndSettlement
import kotlinx.coroutines.flow.Flow

/**
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class YearEndSettlementRepository(private val dao: YearEndSettlementDao) {

    fun getSettlementsForShop(shopId: Int): Flow<List<YearEndSettlement>> =
        dao.getSettlementsForShop(shopId)

    suspend fun getLatestSettlement(shopId: Int): YearEndSettlement? =
        dao.getLatestSettlement(shopId)

    fun getSettlementEntries(settlementId: Int): Flow<List<SettlementEntryWithName>> =
        dao.getSettlementEntries(settlementId)

    suspend fun saveSettlement(
        settlement: YearEndSettlement,
        entries: List<SettlementEntry>
    ): Long {
        val id = dao.insertSettlement(settlement)
        val linkedEntries = entries.map { it.copy(settlementId = id.toInt()) }
        dao.insertSettlementEntries(linkedEntries)
        return id
    }

    suspend fun updateSettlementEntry(entry: SettlementEntry) =
        dao.updateSettlementEntry(entry)

    /**
     * Marks an investor's settlement entry as paid.
     * Converts the [SettlementEntryWithName] projection back to a [SettlementEntry]
     * entity and writes the paid amount + date.
     */
    suspend fun markEntrySettled(
        entry: SettlementEntryWithName,
        paidAmount: Double,
        paidDate: Long
    ) {
        val updated = SettlementEntry(
            id = entry.id,
            settlementId = entry.settlementId,
            investorId = entry.investorId,
            fairShareAmount = entry.fairShareAmount,
            actualPaidAmount = entry.actualPaidAmount,
            balanceAmount = entry.balanceAmount,
            settlementPaidAmount = paidAmount,
            settlementPaidDate = paidDate
        )
        dao.updateSettlementEntry(updated)
    }

    suspend fun deleteSettlement(id: Int) =
        dao.deleteSettlement(id)
}
