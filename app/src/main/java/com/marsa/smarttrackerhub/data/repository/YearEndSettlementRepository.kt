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

    suspend fun deleteSettlement(id: Int) =
        dao.deleteSettlement(id)
}
