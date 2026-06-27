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

    // ── Scoped pulls (per-screen sync) ────────────────────────────────────────

    /** Pull only shops. */
    suspend fun pullShopsScoped(): Boolean {
        if (!ensureSignedIn()) return false
        pullShops()
        return true
    }

    /** Pull only employees (uses local shops to resolve the shop FK). */
    suspend fun pullEmployeesScoped(): Boolean {
        if (!ensureSignedIn()) return false
        pullEmployees(localShopIdMap())
        return true
    }

    /**
     * Pull the WHOLE investor domain: investors + shop-investor links + transactions +
     * settlements + entries. Shops are assumed already present locally (resolved via
     * [localShopIdMap]); this scope does not pull shops/employees.
     */
    suspend fun pullInvestorDomain(): Boolean {
        if (!ensureSignedIn()) return false
        val shopIdMap       = localShopIdMap()
        val investorIdMap   = pullInvestors()
        val siIdMap         = pullShopInvestors(shopIdMap, investorIdMap)
        pullTransactions(siIdMap)
        recomputeShopTotals(shopIdMap)
        val settlementIdMap = pullSettlements(shopIdMap)
        pullSettlementEntries(settlementIdMap, investorIdMap)
        return true
    }

    /**
     * Apply deletion tombstones from `/deletions`: hard-delete the matching local row by
     * Firebase id (idempotent — no-op if already gone). Parent deletes cascade to children
     * via Room FKs. Makes deletes propagate to every device.
     */
    suspend fun pullDeletions(): Boolean {
        if (!ensureSignedIn()) return false
        val docs = firestoreGetCollection("deletions")
        Log.d(tag, "pullDeletions: ${docs.size} tombstone(s) from Firebase")
        for (data in docs) {
            try {
                val collection = data["collection"] as? String ?: continue
                val firebaseId = data["firebaseId"] as? String ?: continue
                if (firebaseId.isBlank()) continue
                // Only delete a local row that is NOT newer than the tombstone. A row that was
                // legitimately re-created (reusing the same id) after this deletion has a larger
                // updatedAt and survives — so an old tombstone can't resurrect-delete it.
                val deletedAt = (data["deletedAt"] as? Long) ?: Long.MAX_VALUE
                when (collection) {
                    "shops"              -> db.shopDao().deleteByFirebaseId(firebaseId, deletedAt)
                    "employees"          -> db.employeeDao().deleteByFirebaseId(firebaseId, deletedAt)
                    "investors"          -> db.investorDao().deleteByFirebaseId(firebaseId, deletedAt)
                    "shop_investors"     -> db.shopInvestorDao().deleteByFirebaseId(firebaseId, deletedAt)
                    "transactions"       -> db.investmentTransactionDao().deleteByFirebaseId(firebaseId, deletedAt)
                    "settlements"        -> db.yearEndSettlementDao().deleteSettlementByFirebaseId(firebaseId, deletedAt)
                    "settlement_entries" -> db.yearEndSettlementDao().deleteEntryByFirebaseId(firebaseId, deletedAt)
                }
            } catch (e: Exception) {
                Log.e(tag, "pullDeletions: error processing tombstone", e)
            }
        }
        return true
    }

    /** shopId(Firebase) → Room id, from LOCAL shops (no network). */
    private suspend fun localShopIdMap(): Map<String, Int> =
        db.shopDao().getAllShopsAsList()
            .filter { it.shopId.isNotBlank() }
            .associate { it.shopId to it.id }

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
                    db.shopDao().updateTotalInvested(roomId, computed, System.currentTimeMillis())
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
                val incomingUpdatedAt = data["updatedAt"] as? Long ?: 0L

                val existing_ = existing[shopId]
                val entity = ShopInfo(
                    id                = existing_?.id ?: 0,
                    shopId            = shopId,
                    shopName          = data["shopName"]     as? String ?: "",
                    shopAddress       = data["shopAddress"]  as? String ?: "",
                    shopType          = data["shopType"]     as? String ?: "",
                    shopStatus        = data["shopStatus"]   as? String ?: "Initial",
                    shopRegion        = data["shopRegion"]   as? String ?: "UAE",
                    zakathStatus      = data["zakathStatus"] as? String ?: "",
                    licenseExpiryDate = data["licenseExpiryDate"] as? Long ?: 0L,
                    shopOpeningDate   = data["shopOpeningDate"]   as? Long ?: 0L,
                    stockValue        = (data["stockValue"] as? Double)
                        ?: (data["stockValue"] as? Long)?.toDouble() ?: 0.0,
                    stockTakenDate    = data["stockTakenDate"] as? Long ?: 0L,
                    totalInvested     = (data["totalInvested"] as? Double)
                        ?: (data["totalInvested"] as? Long)?.toDouble() ?: 0.0,
                    isSynced          = true,
                    updatedAt         = incomingUpdatedAt
                )

                if (existing_ != null) {
                    // NEWEST-WINS: overwrite in place only if the cloud copy is strictly newer;
                    // otherwise keep local (it re-pushes). Never deletes.
                    if (incomingUpdatedAt > existing_.updatedAt) db.shopDao().updateShop(entity)
                    idMap[shopId] = existing_.id
                    continue
                }
                db.shopDao().insertShop(entity)

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
                val incomingUpdatedAt = data["updatedAt"] as? Long ?: 0L

                val existing = db.investorDao().getInvestorByInvestorId(investorFbId)
                val entity = InvestorInfo(
                    id            = existing?.id ?: 0,
                    investorId    = investorFbId,                                  // preserve identity
                    investorName  = data["investorName"]  as? String ?: "",
                    investorEmail = data["investorEmail"]  as? String ?: "",
                    investorPhone = data["investorPhone"]  as? String ?: "",
                    isSynced      = true,                                          // pulled → already in Firestore
                    updatedAt     = incomingUpdatedAt
                )

                if (existing != null) {
                    // NEWEST-WINS: overwrite only if the cloud copy is strictly newer.
                    if (incomingUpdatedAt > existing.updatedAt) db.investorDao().updateInvestor(entity)
                    idMap[investorFbId] = existing.id
                    continue
                }
                db.investorDao().insertInvestor(entity)

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
                val incomingUpdatedAt = data["updatedAt"] as? Long ?: 0L

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
                    isSynced                = true,  // pulled from Firebase → already synced
                    updatedAt               = incomingUpdatedAt
                )
                if (existing_ != null) {
                    // NEWEST-WINS: overwrite only if the cloud copy is strictly newer.
                    if (incomingUpdatedAt > existing_.updatedAt) db.employeeDao().updateEmployee(entity)
                    count++
                    continue
                }
                db.employeeDao().insertEmployee(entity)
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

                val incomingUpdatedAt = data["updatedAt"] as? Long ?: 0L
                val sharePercentage = (data["sharePercentage"] as? Double)
                    ?: (data["sharePercentage"] as? Long)?.toDouble() ?: 0.0

                // Match by Firebase ID, else by shop+investor pair.
                val existing = db.shopInvestorDao().getShopInvestorByFirebaseId(fbId)
                    ?: db.shopInvestorDao().getLinksForInvestor(investorRoomId)
                        .firstOrNull { it.shopId == shopRoomId }

                val entity = ShopInvestor(
                    id                     = existing?.id ?: 0,
                    shopId                 = shopRoomId,
                    investorId             = investorRoomId,
                    sharePercentage        = sharePercentage,
                    status                 = data["status"] as? String ?: "Active",
                    joinedDate             = data["joinedDate"] as? Long ?: System.currentTimeMillis(),
                    shopInvestorFirebaseId = fbId,            // preserve identity
                    isSynced               = true,            // pulled → already in Firestore
                    updatedAt              = incomingUpdatedAt
                )

                if (existing != null) {
                    // NEWEST-WINS: overwrite (share %, status/withdrawal) only if strictly newer.
                    if (incomingUpdatedAt > existing.updatedAt) db.shopInvestorDao().updateShopInvestor(entity)
                    idMap[fbId] = existing.id
                    continue
                }
                db.shopInvestorDao().insertShopInvestor(entity)

                val roomId = db.shopInvestorDao().getShopInvestorByFirebaseId(fbId)?.id
                    ?: db.shopInvestorDao().getLinksForInvestor(investorRoomId)
                        .firstOrNull { it.shopId == shopRoomId }?.id
                    ?: continue
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

        // Dedup by Firebase ID AND by content. Firestore may hold several docs for the
        // SAME payment under different IDs (from earlier buggy syncs); matching on content
        // (link + amount + date + phase + note) collapses them into one local row so a pull
        // can never add duplicates, regardless of how many ID variants exist in Firestore.
        val existing = db.investmentTransactionDao().getAllTransactionsList()
        val existingByFbId = existing
            .filter { it.transactionFirebaseId.isNotBlank() }
            .associateBy { it.transactionFirebaseId }
        fun contentKey(siRoomId: Int, amount: Double, date: Long, phase: String, note: String) =
            "$siRoomId|$amount|$date|${phase.trim()}|${note.trim()}"
        val existingByContent = existing
            .associateBy { contentKey(it.shopInvestorId, it.amount, it.transactionDate, it.phase, it.note) }
        val handledContent = HashSet<String>()

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
                val date  = data["transactionDate"] as? Long ?: 0L
                val phase = data["phase"] as? String ?: ""
                val note  = data["note"] as? String ?: ""
                val incomingUpdatedAt = data["updatedAt"] as? Long ?: 0L
                val key   = contentKey(siRoomId, amount, date, phase, note)

                // Match by Firebase ID (catches edits — txFbId is stable across edits) or by
                // content (collapses duplicate cloud docs).
                val match = existingByFbId[txFbId] ?: existingByContent[key]
                val entity = InvestmentTransaction(
                    id                    = match?.id ?: 0,
                    shopInvestorId        = siRoomId,
                    amount                = amount,
                    transactionDate       = date,
                    phase                 = phase,
                    note                  = note,
                    transactionFirebaseId = txFbId,            // preserve identity
                    shopFirebaseId        = shopFbId,
                    investorFirebaseId    = investorFbId,
                    isSynced              = true,              // pulled → already in Firestore
                    updatedAt             = incomingUpdatedAt
                )
                if (match != null) {
                    // NEWEST-WINS: overwrite the matched row only if the cloud copy is newer.
                    if (incomingUpdatedAt > match.updatedAt) db.investmentTransactionDao().updateTransaction(entity)
                    handledContent.add(key)
                    continue
                }
                if (key in handledContent) continue   // duplicate same-content doc this pass
                db.investmentTransactionDao().insertTransaction(entity)
                handledContent.add(key)
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

        // Dedup by Firebase ID AND content (shop + settlementDate + periodStartDate), so
        // duplicate cloud docs for the same settlement collapse to one row. Duplicate docs'
        // Firebase IDs all map to the kept row's Room id, so their entries still resolve.
        val existing = db.yearEndSettlementDao().getAllSettlementsList()
        fun sKey(shopRoomId: Int, date: Long, periodStart: Long) = "$shopRoomId|$date|$periodStart"
        val existingByContent = existing
            .associateBy { sKey(it.shopId, it.settlementDate, it.periodStartDate) }
        val handledContent = HashMap<String, Int>()  // content key → kept Room id

        val idMap = mutableMapOf<String, Int>()
        for (data in docs) {
            try {
                val fbId       = data["settlementFirebaseId"] as? String ?: continue
                val shopFbId   = data["shopFirebaseId"] as? String ?: ""
                val shopRoomId = shopIdMap[shopFbId] ?: continue
                val date        = data["settlementDate"] as? Long ?: 0L
                val periodStart = data["periodStartDate"] as? Long ?: 0L
                val incomingUpdatedAt = data["updatedAt"] as? Long ?: 0L
                val key         = sKey(shopRoomId, date, periodStart)

                val totalInvested = (data["totalInvested"] as? Double)
                    ?: (data["totalInvested"] as? Long)?.toDouble() ?: 0.0

                val existingRow = db.yearEndSettlementDao().getSettlementByFirebaseId(fbId)
                    ?: existingByContent[key]
                val entity = YearEndSettlement(
                    id                   = existingRow?.id ?: 0,
                    shopId               = shopRoomId,
                    settlementDate       = date,
                    periodStartDate      = periodStart,
                    totalInvested        = totalInvested,
                    note                 = data["note"] as? String ?: "",
                    isCarriedForward     = data["isCarriedForward"] as? Boolean ?: true,
                    settlementFirebaseId = fbId,                                  // preserve identity
                    shopFirebaseId       = shopFbId,
                    isSynced             = true,                                  // pulled → already in Firestore
                    updatedAt            = incomingUpdatedAt
                )

                // NEWEST-WINS + dedup: existing (by Firebase ID or content) → overwrite only if
                // newer, map this fbId to the kept row; same-content already handled → map too.
                if (existingRow != null) {
                    if (incomingUpdatedAt > existingRow.updatedAt) db.yearEndSettlementDao().updateSettlement(entity)
                    idMap[fbId] = existingRow.id
                    continue
                }
                val handledRoomId = handledContent[key]
                if (handledRoomId != null) { idMap[fbId] = handledRoomId; continue }

                db.yearEndSettlementDao().insertSettlement(entity)

                val roomId = db.yearEndSettlementDao().getSettlementByFirebaseId(fbId)?.id ?: continue
                idMap[fbId] = roomId
                handledContent[key] = roomId
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

        // Dedup by Firebase ID AND content (settlement + investor: one entry per investor per
        // settlement). Collapses duplicate cloud docs for the same entry into one local row.
        val allEntries = db.yearEndSettlementDao().getAllSettlementEntriesList()
        val existingByFbId = allEntries
            .filter { it.entryFirebaseId.isNotBlank() }
            .associateBy { it.entryFirebaseId }
        val existingByContent = allEntries
            .associateBy { "${it.settlementId}|${it.investorId}" }
        val handledContent = HashSet<String>()

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
                val incomingUpdatedAt = data["updatedAt"] as? Long ?: 0L
                val contentKey       = "$settlementRoomId|$investorRoomId"

                fun toDouble(key: String) =
                    (data[key] as? Double) ?: (data[key] as? Long)?.toDouble() ?: 0.0

                val match = existingByFbId[entryFbId] ?: existingByContent[contentKey]
                val entity = SettlementEntry(
                    id                   = match?.id ?: 0,
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
                    isSynced             = true,                // pulled → already in Firestore
                    updatedAt            = incomingUpdatedAt
                )

                // NEWEST-WINS: existing (by Firebase ID or content) → overwrite the matched row
                // (e.g. a "mark paid" edit) only if the cloud copy is newer; else keep.
                if (match != null) {
                    if (incomingUpdatedAt > match.updatedAt) db.yearEndSettlementDao().updateSettlementEntry(entity)
                    handledContent.add(contentKey)
                    continue
                }
                if (contentKey in handledContent) continue  // duplicate same-content doc this pass
                db.yearEndSettlementDao().insertSettlementEntries(listOf(entity))
                handledContent.add(contentKey)
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
