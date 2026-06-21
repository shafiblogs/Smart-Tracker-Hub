package com.marsa.smarttrackerhub.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.EmployeeInfo
import com.marsa.smarttrackerhub.data.entity.InvestmentTransaction
import com.marsa.smarttrackerhub.data.entity.InvestorInfo
import com.marsa.smarttrackerhub.data.entity.SettlementEntry
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import com.marsa.smarttrackerhub.data.entity.ShopInvestor
import com.marsa.smarttrackerhub.data.entity.YearEndSettlement
import kotlinx.coroutines.flow.first
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Pulls all 7 Firestore collections → Room in FK-dependency order:
 *
 *  1. /shops               → shop_info
 *  2. /investors           → investor_info
 *  3. /employees           → employee_info
 *  4. /shop_investors      → shop_investor          (FKs: shopId, investorId)
 *  5. /transactions        → investment_transaction (FK: shopInvestorId)
 *  6. /settlements         → year_end_settlement    (FK: shopId)
 *  7. /settlement_entries  → settlement_entry       (FKs: settlementId, investorId)
 *
 * Created by Muhammed Shafi on 20/03/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class FirebasePullRepository(private val db: AppDatabase) {

    private val firestore = FirebaseFirestore.getInstance()
    private val tag = "##FirebasePull"

    // ─────────────────────────────────────────────────────────────────────────
    // Auth
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun ensureSignedIn(): Boolean {
        val auth = FirebaseAuth.getInstance()
        return suspendCoroutine { cont ->
            auth.signInAnonymously()
                .addOnSuccessListener { cont.resume(true) }
                .addOnFailureListener { e ->
                    Log.e(tag, "Anonymous sign-in failed: ${e.message}")
                    cont.resume(false)
                }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Entry point
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun pullAll(): Boolean {
        if (!ensureSignedIn()) {
            Log.w(tag, "pullAll: sign-in failed — skipping pull")
            return false
        }

        Log.d(tag, "pullAll: starting full pull from Firebase")

        val shopIdMap       = pullShops()
        val investorIdMap   = pullInvestors()
        pullEmployees(shopIdMap)
        val siIdMap         = pullShopInvestors(shopIdMap, investorIdMap)
        pullTransactions(siIdMap)
        recomputeShopTotals(shopIdMap)
        val settlementIdMap = pullSettlements(shopIdMap)
        pullSettlementEntries(settlementIdMap, investorIdMap)

        Log.d(tag, "pullAll: complete")
        return true
    }

    /**
     * After transactions are pulled, recompute each shop's cached totalInvested from the
     * actual transaction rows. Keeps the shop card accurate on this device even if the
     * pulled cached value had drifted. Only writes when the value actually changed.
     */
    private suspend fun recomputeShopTotals(shopIdMap: Map<String, Int>) {
        for (roomId in shopIdMap.values) {
            try {
                val computed = db.investmentTransactionDao().getTotalPaidForShop(roomId)
                val current  = db.shopDao().getShopById(roomId)?.totalInvested ?: 0.0
                if (kotlin.math.abs(computed - current) > 0.001) {
                    db.shopDao().updateTotalInvested(roomId, computed)
                    Log.d(tag, "recomputeShopTotals: shop room#$roomId total $current → $computed")
                }
            } catch (e: Exception) {
                Log.e(tag, "recomputeShopTotals: error for shop room#$roomId", e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Shops  (/shops/{shopId})
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun pullShops(): Map<String, Int> {
        val docs = firestoreGetCollection("shops")
        Log.d(tag, "pullShops: ${docs.size} document(s) from Firebase")

        // Build existing map (shopId string → Room entity) to reuse Room int PKs on update
        val existing = db.shopDao().getAllShops().first().associateBy { it.shopId }

        val idMap = mutableMapOf<String, Int>()
        for (data in docs) {
            try {
                val shopId = data["shopId"] as? String ?: continue
                if (shopId.isBlank()) continue

                val existing_ = existing[shopId]
                // If local record has unsynced changes (isSynced = false), trust local shopStatus
                // over whatever Firestore has — the local value is newer and hasn't been pushed yet.
                // This prevents a failed push + successful pull from silently discarding local edits.
                val localHasPendingChanges = existing_ != null && !existing_.isSynced
                val resolvedStatus = if (localHasPendingChanges) {
                    existing_!!.shopStatus
                } else {
                    data["shopStatus"] as? String ?: existing_?.shopStatus ?: "Initial"
                }
                // Keep isSynced = false when we preserved a local value so SyncWorker's
                // second push pass will flush the correct status back to Firestore.
                val resolvedIsSynced = !localHasPendingChanges

                val entity = ShopInfo(
                    id                = existing_?.id ?: 0,
                    shopId            = shopId,
                    shopName          = data["shopName"]     as? String ?: "",
                    shopAddress       = data["shopAddress"]  as? String ?: "",
                    shopType          = data["shopType"]     as? String ?: "",
                    shopStatus        = resolvedStatus,
                    shopRegion        = data["shopRegion"]   as? String ?: "UAE",
                    zakathStatus      = data["zakathStatus"] as? String ?: "",
                    licenseExpiryDate = data["licenseExpiryDate"] as? Long ?: 0L,
                    shopOpeningDate   = data["shopOpeningDate"]   as? Long ?: 0L,
                    stockValue        = (data["stockValue"] as? Double)
                        ?: (data["stockValue"] as? Long)?.toDouble() ?: 0.0,
                    stockTakenDate    = data["stockTakenDate"] as? Long ?: 0L,
                    totalInvested     = (data["totalInvested"] as? Double)
                        ?: (data["totalInvested"] as? Long)?.toDouble() ?: 0.0,
                    isSynced          = resolvedIsSynced
                )
                if (existing_ != null) db.shopDao().updateShop(entity)
                else db.shopDao().insertShop(entity)

                val roomId = db.shopDao().getAllShops().first()
                    .firstOrNull { it.shopId == shopId }?.id ?: continue
                idMap[shopId] = roomId
            } catch (e: Exception) {
                Log.e(tag, "pullShops: error processing document", e)
            }
        }
        Log.d(tag, "pullShops: upserted ${idMap.size} shop(s)")
        return idMap
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Investors  (/investors/{investorId})
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun pullInvestors(): Map<String, Int> {
        val docs = firestoreGetCollection("investors")
        Log.d(tag, "pullInvestors: ${docs.size} document(s) from Firebase")

        val idMap = mutableMapOf<String, Int>()
        for (data in docs) {
            try {
                val investorFbId  = data["investorId"] as? String ?: continue
                if (investorFbId.isBlank()) continue

                // Match existing rows by Firebase ID (consistent with pullShops/pullEmployees),
                // not by email — email can be blank, shared, or edited.
                val existing = db.investorDao().getInvestorByInvestorId(investorFbId)

                val entity = InvestorInfo(
                    id            = existing?.id ?: 0,
                    investorId    = investorFbId,                                  // preserve identity
                    investorName  = data["investorName"]  as? String ?: "",
                    investorEmail = data["investorEmail"]  as? String ?: "",
                    investorPhone = data["investorPhone"]  as? String ?: "",
                    isSynced      = true                                           // pulled → already in Firestore
                )
                if (existing != null) db.investorDao().updateInvestor(entity)
                else db.investorDao().insertInvestor(entity)

                val roomId = db.investorDao().getInvestorByInvestorId(investorFbId)?.id ?: continue
                idMap[investorFbId] = roomId
            } catch (e: Exception) {
                Log.e(tag, "pullInvestors: error processing document", e)
            }
        }
        Log.d(tag, "pullInvestors: upserted ${idMap.size} investor(s)")
        return idMap
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Employees  (/employees/{employeeId})
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun pullEmployees(shopIdMap: Map<String, Int>) {
        val docs = firestoreGetCollection("employees")
        Log.d(tag, "pullEmployees: ${docs.size} document(s) from Firebase")

        // Build existing map (employeeId string → entity) to reuse Room int PKs on update
        val existing = db.employeeDao().getAllEmployeesAsList().associateBy { it.employeeId }

        var count = 0
        for (data in docs) {
            try {
                val employeeId = data["employeeId"] as? String ?: continue
                if (employeeId.isBlank()) continue

                val shopFirebaseId = data["associatedShopFirebaseId"] as? String ?: ""
                val shopRoomId     = shopIdMap[shopFirebaseId] ?: 0

                val existing_ = existing[employeeId]
                val entity = EmployeeInfo(
                    id                      = existing_?.id ?: 0,
                    employeeId              = employeeId,
                    employeeName            = data["employeeName"]   as? String ?: "",
                    employeePhone           = data["employeePhone"]  as? String ?: "",
                    employeeRole            = data["employeeRole"]   as? String ?: "",
                    salary                  = (data["salary"]    as? Double) ?: (data["salary"]    as? Long)?.toDouble() ?: 0.0,
                    allowance               = (data["allowance"] as? Double) ?: (data["allowance"] as? Long)?.toDouble() ?: 0.0,
                    associatedShopId        = shopRoomId,
                    associatedShopFirebaseId = shopFirebaseId,
                    visaExpiryDate          = data["visaExpiryDate"] as? Long ?: 0L,
                    isActive                = data["isActive"] as? Boolean ?: true,
                    terminationDate         = data["terminationDate"] as? Long,
                    isSynced                = true   // pulled from Firebase → already synced
                )
                if (existing_ != null) db.employeeDao().updateEmployee(entity)
                else db.employeeDao().insertEmployee(entity)
                count++
            } catch (e: Exception) {
                Log.e(tag, "pullEmployees: error processing document", e)
            }
        }
        Log.d(tag, "pullEmployees: upserted $count employee(s)")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. ShopInvestors  (/shop_investors/{shopInvestorFirebaseId})
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun pullShopInvestors(
        shopIdMap: Map<String, Int>,
        investorIdMap: Map<String, Int>
    ): Map<String, Int> {
        val docs = firestoreGetCollection("shop_investors")
        Log.d(tag, "pullShopInvestors: ${docs.size} document(s) from Firebase")

        val idMap = mutableMapOf<String, Int>()
        for (data in docs) {
            try {
                val fbId         = data["shopInvestorFirebaseId"] as? String ?: continue
                val shopFbId     = data["shopFirebaseId"] as? String ?: ""
                val investorFbId = data["investorFirebaseId"] as? String ?: ""

                val shopRoomId     = shopIdMap[shopFbId] ?: continue
                val investorRoomId = investorIdMap[investorFbId] ?: continue

                // Check if this shop-investor link already exists
                val alreadyLinked = db.shopInvestorDao().isInvestorInShop(shopRoomId, investorRoomId) > 0
                if (alreadyLinked) {
                    val roomId = db.shopInvestorDao().getActiveInvestorsRaw(shopRoomId)
                        .firstOrNull { it.investorId == investorRoomId }?.id ?: continue
                    idMap[fbId] = roomId
                    continue
                }

                val sharePercentage = (data["sharePercentage"] as? Double)
                    ?: (data["sharePercentage"] as? Long)?.toDouble() ?: 0.0

                val entity = ShopInvestor(
                    id                     = 0,
                    shopId                 = shopRoomId,
                    investorId             = investorRoomId,
                    sharePercentage        = sharePercentage,
                    status                 = data["status"] as? String ?: "Active",
                    joinedDate             = data["joinedDate"] as? Long ?: System.currentTimeMillis(),
                    shopInvestorFirebaseId = fbId,            // preserve identity
                    isSynced               = true             // pulled → already in Firestore
                )
                db.shopInvestorDao().insertShopInvestor(entity)

                val roomId = db.shopInvestorDao().getActiveInvestorsRaw(shopRoomId)
                    .firstOrNull { it.investorId == investorRoomId }?.id ?: continue
                idMap[fbId] = roomId
            } catch (e: Exception) {
                Log.e(tag, "pullShopInvestors: error processing document", e)
            }
        }
        Log.d(tag, "pullShopInvestors: upserted ${idMap.size} shop-investor link(s)")
        return idMap
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Investment Transactions  (/transactions/{transactionFirebaseId})
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun pullTransactions(siIdMap: Map<String, Int>) {
        val docs = firestoreGetCollection("transactions")
        Log.d(tag, "pullTransactions: ${docs.size} document(s) from Firebase")

        // Existing transactions keyed by their UUID Firebase ID — reuse Room PKs so a
        // re-pull updates in place instead of inserting (and so the row is never re-pushed
        // under a NEW UUID, which would duplicate the doc in Firestore).
        val existingByFbId = db.investmentTransactionDao().getAllTransactionsList()
            .filter { it.transactionFirebaseId.isNotBlank() }
            .associateBy { it.transactionFirebaseId }

        var count = 0
        for (data in docs) {
            try {
                val txFbId       = data["transactionFirebaseId"] as? String ?: continue
                if (txFbId.isBlank()) continue
                val shopFbId     = data["shopFirebaseId"] as? String ?: ""
                val investorFbId = data["investorFirebaseId"] as? String ?: ""
                val siFirebaseId = "${shopFbId}_${investorFbId}"
                val siRoomId     = siIdMap[siFirebaseId] ?: continue

                val amount = (data["amount"] as? Double)
                    ?: (data["amount"] as? Long)?.toDouble() ?: 0.0

                val entity = InvestmentTransaction(
                    id                    = existingByFbId[txFbId]?.id ?: 0,
                    shopInvestorId        = siRoomId,
                    amount                = amount,
                    transactionDate       = data["transactionDate"] as? Long ?: 0L,
                    phase                 = data["phase"] as? String ?: "",
                    note                  = data["note"] as? String ?: "",
                    transactionFirebaseId = txFbId,            // preserve identity
                    shopFirebaseId        = shopFbId,
                    investorFirebaseId    = investorFbId,
                    isSynced              = true               // pulled → already in Firestore
                )
                db.investmentTransactionDao().insertTransaction(entity)
                count++
            } catch (e: Exception) {
                Log.e(tag, "pullTransactions: error processing document", e)
            }
        }
        Log.d(tag, "pullTransactions: upserted $count transaction(s)")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Settlements  (/settlements/{settlementFirebaseId})
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun pullSettlements(shopIdMap: Map<String, Int>): Map<String, Int> {
        val docs = firestoreGetCollection("settlements")
        Log.d(tag, "pullSettlements: ${docs.size} document(s) from Firebase")

        val idMap = mutableMapOf<String, Int>()
        for (data in docs) {
            try {
                val fbId       = data["settlementFirebaseId"] as? String ?: continue
                val shopFbId   = data["shopFirebaseId"] as? String ?: ""
                val shopRoomId = shopIdMap[shopFbId] ?: continue

                val totalInvested = (data["totalInvested"] as? Double)
                    ?: (data["totalInvested"] as? Long)?.toDouble() ?: 0.0

                // Reuse the existing Room PK when this settlement was already pulled,
                // so re-pulls update in place instead of inserting duplicates.
                val existingRoomId = db.yearEndSettlementDao().getSettlementByFirebaseId(fbId)?.id ?: 0

                val entity = YearEndSettlement(
                    id                   = existingRoomId,
                    shopId               = shopRoomId,
                    settlementDate       = data["settlementDate"] as? Long ?: 0L,
                    periodStartDate      = data["periodStartDate"] as? Long ?: 0L,
                    totalInvested        = totalInvested,
                    note                 = data["note"] as? String ?: "",
                    isCarriedForward     = data["isCarriedForward"] as? Boolean ?: true,
                    settlementFirebaseId = fbId,                                  // preserve identity
                    shopFirebaseId       = shopFbId,
                    isSynced             = true                                   // pulled → already in Firestore
                )
                db.yearEndSettlementDao().insertSettlement(entity)

                // Map by Firebase ID lookup — NOT getLatestSettlement, which mis-maps
                // when a shop has more than one settlement.
                val roomId = db.yearEndSettlementDao().getSettlementByFirebaseId(fbId)?.id ?: continue
                idMap[fbId] = roomId
            } catch (e: Exception) {
                Log.e(tag, "pullSettlements: error processing document", e)
            }
        }
        Log.d(tag, "pullSettlements: upserted ${idMap.size} settlement(s)")
        return idMap
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. Settlement Entries  (/settlement_entries/{entryFirebaseId})
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun pullSettlementEntries(
        settlementIdMap: Map<String, Int>,
        investorIdMap: Map<String, Int>
    ) {
        val docs = firestoreGetCollection("settlement_entries")
        Log.d(tag, "pullSettlementEntries: ${docs.size} document(s) from Firebase")

        // Existing entries keyed by UUID Firebase ID — reuse Room PKs so re-pulls update in
        // place and the row is never re-pushed under a new UUID (which would duplicate).
        val existingByFbId = db.yearEndSettlementDao().getAllSettlementEntriesList()
            .filter { it.entryFirebaseId.isNotBlank() }
            .associateBy { it.entryFirebaseId }

        var count = 0
        for (data in docs) {
            try {
                val entryFbId      = data["entryFirebaseId"] as? String ?: continue
                if (entryFbId.isBlank()) continue
                val settlementFbId = data["settlementFirebaseId"] as? String ?: ""
                val investorFbId   = data["investorFirebaseId"] as? String ?: ""
                val shopFbId       = data["shopFirebaseId"] as? String ?: ""

                val settlementRoomId = settlementIdMap[settlementFbId] ?: continue
                val investorRoomId   = investorIdMap[investorFbId] ?: continue

                fun toDouble(key: String) =
                    (data[key] as? Double) ?: (data[key] as? Long)?.toDouble() ?: 0.0

                val entity = SettlementEntry(
                    id                   = existingByFbId[entryFbId]?.id ?: 0,
                    settlementId         = settlementRoomId,
                    investorId           = investorRoomId,
                    fairShareAmount      = toDouble("fairShareAmount"),
                    actualPaidAmount     = toDouble("actualPaidAmount"),
                    balanceAmount        = toDouble("balanceAmount"),
                    settlementPaidAmount = toDouble("settlementPaidAmount"),
                    settlementPaidDate   = data["settlementPaidDate"] as? Long,
                    entryFirebaseId      = entryFbId,            // preserve identity
                    investorFirebaseId   = investorFbId,
                    settlementFirebaseId = settlementFbId,
                    shopFirebaseId       = shopFbId,
                    isSynced             = true                 // pulled → already in Firestore
                )
                db.yearEndSettlementDao().insertSettlementEntries(listOf(entity))
                count++
            } catch (e: Exception) {
                Log.e(tag, "pullSettlementEntries: error processing document", e)
            }
        }
        Log.d(tag, "pullSettlementEntries: upserted $count entry/entries")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun firestoreGetCollection(collection: String): List<Map<String, Any>> =
        suspendCoroutine { cont ->
            firestore.collection(collection)
                .get()
                .addOnSuccessListener { snapshot ->
                    cont.resume(snapshot.documents.mapNotNull { it.data })
                }
                .addOnFailureListener { e ->
                    Log.e(tag, "Failed to pull $collection: ${e.message}")
                    cont.resume(emptyList())
                }
        }
}
