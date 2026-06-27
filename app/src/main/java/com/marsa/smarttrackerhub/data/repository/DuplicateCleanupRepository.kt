package com.marsa.smarttrackerhub.data.repository

import android.util.Log
import com.marsa.smarttrackerhub.data.AppDatabase
import java.util.UUID

/**
 * One-time LOCAL-ONLY cleanup that removes duplicate rows created by the sync bug
 * (records whose Firebase IDs weren't stable across syncs, so each sync inserted copies).
 *
 * IMPORTANT: This touches ONLY the local Room database. It does NOT read or write Firestore.
 * After running it, the device holds a single clean copy; the caller then wipes Firestore
 * and does one clean sync to re-upload.
 *
 * Dedup strategy (FK-safe order): investors → links → transactions → settlements → entries.
 * Within each group the lowest Room id is kept (the original, created first) and child rows
 * are re-pointed onto it before the duplicate is deleted, so no real data is lost.
 *
 * Created by Claude on behalf of Muhammed Shafi.
 */
class DuplicateCleanupRepository(private val db: AppDatabase) {

    data class Result(
        val investorsRemoved: Int,
        val linksRemoved: Int,
        val transactionsRemoved: Int,
        val settlementsRemoved: Int,
        val entriesRemoved: Int
    )

    private val tag = "##DedupCleanup"

    suspend fun removeDuplicates(): Result {
        var investorsRemoved = 0
        var linksRemoved = 0
        var transactionsRemoved = 0
        var settlementsRemoved = 0
        var entriesRemoved = 0

        // 1) Investors — group by identity (name|phone|email). Keep lowest id, re-point its
        //    links + settlement entries, delete the duplicates (this also clears the orphan
        //    "0 shops" copies, since they share the same identity as the original).
        val investors = db.investorDao().getAllInvestorsAsList()
        investors.groupBy {
            listOf(
                it.investorName.trim().lowercase(),
                it.investorPhone.trim(),
                it.investorEmail.trim().lowercase()
            ).joinToString("|")
        }.forEach { (_, group) ->
            if (group.size <= 1) return@forEach
            val keep = group.minByOrNull { it.id }!!
            group.filter { it.id != keep.id }.forEach { dup ->
                db.shopInvestorDao().repointInvestor(dup.id, keep.id)
                db.yearEndSettlementDao().repointEntryInvestor(dup.id, keep.id)
                db.investorDao().deleteInvestor(dup)
                investorsRemoved++
            }
        }

        // 2) Shop-investor links — group by (shopId, investorId). Keep lowest id, re-point
        //    transactions, delete duplicates.
        val links = db.shopInvestorDao().getAllShopInvestorsAsList()
        links.groupBy { it.shopId to it.investorId }.forEach { (_, group) ->
            if (group.size <= 1) return@forEach
            val keep = group.minByOrNull { it.id }!!
            group.filter { it.id != keep.id }.forEach { dup ->
                db.investmentTransactionDao().repointShopInvestor(dup.id, keep.id)
                db.shopInvestorDao().deleteShopInvestorById(dup.id)
                linksRemoved++
            }
        }

        // 3) Transactions — group by all business fields. Keep lowest id, delete the copies.
        val txns = db.investmentTransactionDao().getAllTransactionsList()
        txns.groupBy {
            listOf(it.shopInvestorId, it.amount, it.transactionDate, it.phase.trim(), it.note.trim())
                .joinToString("|")
        }.forEach { (_, group) ->
            if (group.size <= 1) return@forEach
            val keep = group.minByOrNull { it.id }!!
            group.filter { it.id != keep.id }.forEach { dup ->
                db.investmentTransactionDao().deleteTransactionById(dup.id)
                transactionsRemoved++
            }
        }

        // 4) Settlements — group by (shopId, settlementDate, periodStartDate).
        val settlements = db.yearEndSettlementDao().getAllSettlementsList()
        settlements.groupBy {
            listOf(it.shopId, it.settlementDate, it.periodStartDate).joinToString("|")
        }.forEach { (_, group) ->
            if (group.size <= 1) return@forEach
            val keep = group.minByOrNull { it.id }!!
            group.filter { it.id != keep.id }.forEach { dup ->
                db.yearEndSettlementDao().repointEntrySettlement(dup.id, keep.id)
                db.yearEndSettlementDao().deleteSettlement(dup.id)
                settlementsRemoved++
            }
        }

        // 5) Settlement entries — group by (settlementId, investorId).
        val entries = db.yearEndSettlementDao().getAllSettlementEntriesList()
        entries.groupBy { it.settlementId to it.investorId }.forEach { (_, group) ->
            if (group.size <= 1) return@forEach
            val keep = group.minByOrNull { it.id }!!
            group.filter { it.id != keep.id }.forEach { dup ->
                db.yearEndSettlementDao().deleteSettlementEntryById(dup.id)
                entriesRemoved++
            }
        }

        // 6) Stamp stable Firebase IDs onto any blank rows and persist them locally NOW, so
        //    the next sync uses fixed IDs and cannot mint a fresh one each time (the root
        //    cause of the duplication). Order matters: investors → links → transactions.
        db.investorDao().getAllInvestorsAsList()
            .filter { it.investorId.isBlank() }
            .forEach { inv ->
                db.investorDao().updateInvestor(inv.copy(investorId = UUID.randomUUID().toString()))
            }

        db.shopInvestorDao().getAllShopInvestorsAsList()
            .filter { it.shopInvestorFirebaseId.isBlank() }
            .forEach { link ->
                val shopFb = db.shopDao().getShopById(link.shopId)?.shopId ?: ""
                val invFb = db.investorDao().getInvestorById(link.investorId)?.investorId ?: ""
                if (shopFb.isNotBlank() && invFb.isNotBlank()) {
                    db.shopInvestorDao().updateShopInvestor(
                        link.copy(shopInvestorFirebaseId = "${shopFb}_${invFb}")
                    )
                }
            }

        db.investmentTransactionDao().getAllTransactionsList()
            .filter { it.transactionFirebaseId.isBlank() || it.shopFirebaseId.isBlank() || it.investorFirebaseId.isBlank() }
            .forEach { tx ->
                val link = db.shopInvestorDao().getShopInvestorById(tx.shopInvestorId)
                val shopFb = tx.shopFirebaseId.ifBlank { link?.let { db.shopDao().getShopById(it.shopId)?.shopId } ?: "" }
                val invFb = tx.investorFirebaseId.ifBlank { link?.let { db.investorDao().getInvestorById(it.investorId)?.investorId } ?: "" }
                db.investmentTransactionDao().updateTransaction(
                    tx.copy(
                        transactionFirebaseId = tx.transactionFirebaseId.ifBlank { UUID.randomUUID().toString() },
                        shopFirebaseId = shopFb,
                        investorFirebaseId = invFb
                    )
                )
            }

        // 7) Recompute each shop's cached total from the de-duped transactions.
        db.shopDao().getAllShopsAsList().forEach { shop ->
            val total = db.investmentTransactionDao().getTotalPaidForShop(shop.id)
            db.shopDao().updateTotalInvested(shop.id, total, System.currentTimeMillis())
        }

        val result = Result(investorsRemoved, linksRemoved, transactionsRemoved, settlementsRemoved, entriesRemoved)
        Log.d(tag, "removeDuplicates: $result")
        return result
    }
}
