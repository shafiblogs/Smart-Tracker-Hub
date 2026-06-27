package com.marsa.smarttrackerhub.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.EmployeeInfo
import com.marsa.smarttrackerhub.data.entity.InvestmentTransaction
import com.marsa.smarttrackerhub.data.entity.InvestorInfo
import com.marsa.smarttrackerhub.data.entity.SettlementEntry
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import com.marsa.smarttrackerhub.data.entity.ShopInvestor
import com.marsa.smarttrackerhub.data.entity.YearEndSettlement
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Handles one-way write sync from Room → Firestore for all 7 push-able tables.
 *
 * Sync strategy:
 *  - Each entity carries `isSynced = false` until successfully pushed.
 *  - ViewModels call `syncXxx()` immediately after a Room write (best-effort, silent failure).
 *  - [SyncWorker] calls [syncAll] periodically to retry any remaining `isSynced=false` records.
 *
 * Firestore collection layout (flat, top-level):
 *  /shops/{shopId}                          ← ShopInfo
 *  /investors/{investorId}                  ← InvestorInfo
 *  /employees/{employeeId}                  ← EmployeeInfo
 *  /shop_investors/{shopInvestorFirebaseId} ← ShopInvestor
 *  /transactions/{transactionFirebaseId}    ← InvestmentTransaction
 *  /settlements/{settlementFirebaseId}      ← YearEndSettlement
 *  /settlement_entries/{entryFirebaseId}    ← SettlementEntry
 *
 * Created by Muhammed Shafi on 13/03/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class FirebaseSyncRepository(private val db: AppDatabase) {

    private val firestore = FirebaseFirestore.getInstance()
    private val tag = "##FirebaseSync"

    // ─────────────────────────────────────────────────────────────────────────
    // Auth
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun ensureSignedIn(): Boolean {
        val auth = FirebaseAuth.getInstance()
        // Always call signInAnonymously — Firebase returns the existing user if already signed in
        // but also refreshes the token, preventing stale-token auth failures.
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
    // Shop
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Never push `updatedAt = 0`. The bulk worker path reads DB rows that already carry a real
     * stamp (>0), but inline ViewModel syncs pass un-stamped in-memory entities (updatedAt=0).
     * A 0 would tie/lose newest-wins on other devices, silently dropping the edit — so coalesce
     * it to "now" at push time. (The worker's later DB-read re-push remains the source of truth.)
     */
    private fun Long.orNow(): Long = if (this > 0L) this else System.currentTimeMillis()

    suspend fun syncShop(entity: ShopInfo): Boolean {
        // Self-heal a blank/invalid shopId (used as the Firestore doc id) by assigning a
        // UUID and persisting it — instead of silently marking it synced and never pushing.
        val resolved = if (!isValidDocId(entity.shopId)) {
            val updated = entity.copy(shopId = UUID.randomUUID().toString())
            db.shopDao().updateShop(updated)
            updated
        } else {
            entity
        }
        if (!ensureSignedIn()) {
            Log.e(tag, "syncShop: auth failed for ${resolved.shopId} — status '${resolved.shopStatus}' NOT pushed")
            return false
        }
        Log.d(tag, "syncShop: pushing ${resolved.shopId} with status='${resolved.shopStatus}'")

        val map = mapOf(
            "shopId"           to resolved.shopId,
            "shopName"         to resolved.shopName,
            "shopAddress"      to resolved.shopAddress,
            "shopType"         to resolved.shopType,
            "shopStatus"       to resolved.shopStatus,
            "zakathStatus"     to resolved.zakathStatus,
            "licenseExpiryDate" to resolved.licenseExpiryDate,
            "shopOpeningDate"  to resolved.shopOpeningDate,
            "stockValue"       to resolved.stockValue,
            "stockTakenDate"   to resolved.stockTakenDate,
            "totalInvested"    to resolved.totalInvested,
            "shopRegion"       to resolved.shopRegion,
            "updatedAt"        to resolved.updatedAt.orNow()
        )

        val success = firestoreSet("shops", resolved.shopId, map)
        if (success) db.shopDao().markShopSynced(resolved.shopId)
        return success
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Investor
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun syncInvestor(entity: InvestorInfo): Boolean {
        // The investorId is used as the Firestore document id. If it's blank OR not a valid
        // doc id (e.g. contains "/"), assign a UUID and persist it — otherwise the write
        // throws and this investor (and all its children) never sync.
        val resolved = if (!isValidDocId(entity.investorId)) {
            val updated = entity.copy(investorId = UUID.randomUUID().toString())
            db.investorDao().updateInvestor(updated)
            updated
        } else {
            entity
        }
        if (!ensureSignedIn()) return false

        val map = mapOf(
            "investorId"    to resolved.investorId,
            "investorName"  to resolved.investorName,
            "investorEmail" to resolved.investorEmail,
            "investorPhone" to resolved.investorPhone,
            "updatedAt"     to resolved.updatedAt.orNow()
        )

        val success = firestoreSet("investors", resolved.investorId, map)
        if (success) db.investorDao().markInvestorSynced(resolved.investorId)
        return success
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Employee
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun syncEmployee(entity: EmployeeInfo): Boolean {
        // If employeeId is blank (pre-v5 rows migrated with DEFAULT ''), auto-assign a UUID
        // so the employee is properly pushed to Firestore instead of silently skipped.
        val resolvedEntity = if (entity.employeeId.isBlank()) {
            val newId = UUID.randomUUID().toString()
            val updated = entity.copy(employeeId = newId)
            db.employeeDao().updateEmployee(updated)
            updated
        } else {
            entity
        }
        // If associatedShopFirebaseId is blank (pre-v7 migration default),
        // resolve it from the local Room shop record so SmartTracker can find
        // the employee via whereEqualTo("associatedShopFirebaseId", shopId).
        val shopFirebaseId = if (resolvedEntity.associatedShopFirebaseId.isBlank()
            && resolvedEntity.associatedShopId > 0
        ) {
            val shopId = db.shopDao().getShopById(resolvedEntity.associatedShopId)?.shopId ?: ""
            if (shopId.isNotBlank()) {
                val withShop = resolvedEntity.copy(associatedShopFirebaseId = shopId)
                db.employeeDao().updateEmployee(withShop)
            }
            shopId
        } else {
            resolvedEntity.associatedShopFirebaseId
        }

        if (!ensureSignedIn()) return false

        val map = mapOf(
            "employeeId"               to resolvedEntity.employeeId,
            "employeeName"             to resolvedEntity.employeeName,
            "employeePhone"            to resolvedEntity.employeePhone,
            "employeeRole"             to resolvedEntity.employeeRole,
            "salary"                   to resolvedEntity.salary,
            "allowance"                to resolvedEntity.allowance,
            "associatedShopFirebaseId" to shopFirebaseId,
            "visaExpiryDate"           to resolvedEntity.visaExpiryDate,
            "isActive"                 to resolvedEntity.isActive,
            "updatedAt"                to resolvedEntity.updatedAt.orNow()
        )

        val success = firestoreSet("employees", resolvedEntity.employeeId, map)
        if (success) db.employeeDao().markEmployeeSynced(resolvedEntity.employeeId)
        return success
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ShopInvestor
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun syncShopInvestor(entity: ShopInvestor): Boolean {
        // Resolve Firebase string IDs from Room local-int references.
        // syncAll() runs shops + investors before shop-investors, so parents have IDs by now.
        val shopFirebaseId     = db.shopDao().getShopById(entity.shopId)?.shopId ?: ""
        val investorFirebaseId = db.investorDao().getInvestorById(entity.investorId)?.investorId ?: ""

        // Cannot build a valid composite doc id without both parent IDs — leave for a later
        // pass (after the parents have been synced) rather than marking it permanently synced.
        if (shopFirebaseId.isBlank() || investorFirebaseId.isBlank()) {
            Log.w(tag, "syncShopInvestor: parent IDs not ready (shop='$shopFirebaseId', investor='$investorFirebaseId') — deferring")
            return false
        }

        // Legacy rows have a blank composite id — build it from the parents and persist.
        val resolved = if (entity.shopInvestorFirebaseId.isBlank()) {
            val updated = entity.copy(shopInvestorFirebaseId = "${shopFirebaseId}_${investorFirebaseId}")
            db.shopInvestorDao().updateShopInvestor(updated)
            updated
        } else {
            entity
        }
        if (!ensureSignedIn()) return false

        val map = mapOf(
            "shopInvestorFirebaseId" to resolved.shopInvestorFirebaseId,
            "shopFirebaseId"         to shopFirebaseId,
            "investorFirebaseId"     to investorFirebaseId,
            "sharePercentage"        to resolved.sharePercentage,
            "status"                 to resolved.status,
            "joinedDate"             to resolved.joinedDate,
            "updatedAt"              to resolved.updatedAt.orNow()
        )

        val success = firestoreSet("shop_investors", resolved.shopInvestorFirebaseId, map)
        if (success) db.shopInvestorDao().markShopInvestorSynced(resolved.shopInvestorFirebaseId)
        return success
    }

    // ─────────────────────────────────────────────────────────────────────────
    // InvestmentTransaction
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun syncTransaction(entity: InvestmentTransaction): Boolean {
        // Resolve denormalized Firebase IDs from the parent shop-investor link.
        // Legacy rows were inserted before these fields existed, so they are blank.
        val si = db.shopInvestorDao().getShopInvestorById(entity.shopInvestorId)
        val shopFirebaseId = entity.shopFirebaseId.ifBlank {
            si?.let { db.shopDao().getShopById(it.shopId)?.shopId } ?: ""
        }
        val investorFirebaseId = entity.investorFirebaseId.ifBlank {
            si?.let { db.investorDao().getInvestorById(it.investorId)?.investorId } ?: ""
        }

        if (shopFirebaseId.isBlank() || investorFirebaseId.isBlank()) {
            Log.w(tag, "syncTransaction: parent IDs not ready — deferring tx id=${entity.id}")
            return false
        }

        // Generate a UUID doc id for legacy rows and persist all resolved fields.
        val resolved = if (entity.transactionFirebaseId.isBlank()
            || entity.shopFirebaseId.isBlank()
            || entity.investorFirebaseId.isBlank()
        ) {
            val updated = entity.copy(
                transactionFirebaseId = entity.transactionFirebaseId.ifBlank { UUID.randomUUID().toString() },
                shopFirebaseId        = shopFirebaseId,
                investorFirebaseId    = investorFirebaseId
            )
            db.investmentTransactionDao().updateTransaction(updated)
            updated
        } else {
            entity
        }
        if (!ensureSignedIn()) return false

        val map = mapOf(
            "transactionFirebaseId" to resolved.transactionFirebaseId,
            "shopFirebaseId"        to resolved.shopFirebaseId,
            "investorFirebaseId"    to resolved.investorFirebaseId,
            "amount"                to resolved.amount,
            "transactionDate"       to resolved.transactionDate,
            "phase"                 to resolved.phase,
            "note"                  to resolved.note,
            "updatedAt"             to resolved.updatedAt.orNow()
        )

        val success = firestoreSet("transactions", resolved.transactionFirebaseId, map)
        if (success) db.investmentTransactionDao().markTransactionSynced(resolved.transactionFirebaseId)
        return success
    }

    // ─────────────────────────────────────────────────────────────────────────
    // YearEndSettlement
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun syncSettlement(entity: YearEndSettlement): Boolean {
        // Resolve denormalized shop Firebase ID from the Room shop reference (blank on legacy rows).
        val shopFirebaseId = entity.shopFirebaseId.ifBlank {
            db.shopDao().getShopById(entity.shopId)?.shopId ?: ""
        }
        if (shopFirebaseId.isBlank()) {
            Log.w(tag, "syncSettlement: shop ID not ready — deferring settlement id=${entity.id}")
            return false
        }

        // Generate a UUID doc id for legacy rows and persist resolved fields.
        val resolved = if (entity.settlementFirebaseId.isBlank() || entity.shopFirebaseId.isBlank()) {
            val updated = entity.copy(
                settlementFirebaseId = entity.settlementFirebaseId.ifBlank { UUID.randomUUID().toString() },
                shopFirebaseId       = shopFirebaseId
            )
            db.yearEndSettlementDao().updateSettlement(updated)
            updated
        } else {
            entity
        }
        if (!ensureSignedIn()) return false

        val map = mapOf(
            "settlementFirebaseId" to resolved.settlementFirebaseId,
            "shopFirebaseId"       to resolved.shopFirebaseId,
            "settlementDate"       to resolved.settlementDate,
            "periodStartDate"      to resolved.periodStartDate,
            "totalInvested"        to resolved.totalInvested,
            "note"                 to resolved.note,
            "isCarriedForward"     to resolved.isCarriedForward,
            "updatedAt"            to resolved.updatedAt.orNow()
        )

        val success = firestoreSet("settlements", resolved.settlementFirebaseId, map)
        if (success) db.yearEndSettlementDao().markSettlementSynced(resolved.settlementFirebaseId)
        return success
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SettlementEntry
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun syncSettlementEntry(entity: SettlementEntry): Boolean {
        // Resolve denormalized Firebase IDs from the parent settlement + investor (blank on legacy rows).
        val parent = db.yearEndSettlementDao().getSettlementById(entity.settlementId)
        val settlementFirebaseId = entity.settlementFirebaseId.ifBlank { parent?.settlementFirebaseId ?: "" }
        val shopFirebaseId = entity.shopFirebaseId.ifBlank { parent?.shopFirebaseId ?: "" }
        val investorFirebaseId = entity.investorFirebaseId.ifBlank {
            db.investorDao().getInvestorById(entity.investorId)?.investorId ?: ""
        }

        if (settlementFirebaseId.isBlank() || shopFirebaseId.isBlank() || investorFirebaseId.isBlank()) {
            Log.w(tag, "syncSettlementEntry: parent IDs not ready — deferring entry id=${entity.id}")
            return false
        }

        // Generate a UUID doc id for legacy rows and persist resolved fields.
        val resolved = if (entity.entryFirebaseId.isBlank()
            || entity.settlementFirebaseId.isBlank()
            || entity.shopFirebaseId.isBlank()
            || entity.investorFirebaseId.isBlank()
        ) {
            val updated = entity.copy(
                entryFirebaseId      = entity.entryFirebaseId.ifBlank { UUID.randomUUID().toString() },
                settlementFirebaseId = settlementFirebaseId,
                shopFirebaseId       = shopFirebaseId,
                investorFirebaseId   = investorFirebaseId
            )
            db.yearEndSettlementDao().updateSettlementEntry(updated)
            updated
        } else {
            entity
        }
        if (!ensureSignedIn()) return false

        val map = mapOf(
            "entryFirebaseId"      to resolved.entryFirebaseId,
            "settlementFirebaseId" to resolved.settlementFirebaseId,
            "shopFirebaseId"       to resolved.shopFirebaseId,
            "investorFirebaseId"   to resolved.investorFirebaseId,
            "fairShareAmount"      to resolved.fairShareAmount,
            "actualPaidAmount"     to resolved.actualPaidAmount,
            "balanceAmount"        to resolved.balanceAmount,
            "settlementPaidAmount" to resolved.settlementPaidAmount,
            "settlementPaidDate"   to resolved.settlementPaidDate,
            "updatedAt"            to resolved.updatedAt.orNow()
        )

        val success = firestoreSet("settlement_entries", resolved.entryFirebaseId, map)
        if (success) db.yearEndSettlementDao().markSettlementEntrySynced(resolved.entryFirebaseId)
        return success
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bulk retry — called by SyncWorker
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Pushes all unsynced records to Firestore in dependency order:
     * Shops → Investors → Employees → ShopInvestors → Transactions → Settlements → Entries
     *
     * Room queries run first (local, no network). If nothing is pending the function
     * returns immediately — Firebase Auth and Firestore are never contacted.
     * Each failure is logged but does not stop processing the remaining records.
     */
    suspend fun syncAll() {
        // Full push, scope by scope. Order matters for FK resolution: shops → investors →
        // employees → investor children.
        pushShops()
        pushInvestorDomain()   // investors + links + transactions + settlements + entries
        pushEmployees()
        Log.d(tag, "syncAll() completed")
    }

    /** Push only unsynced SHOP records. */
    suspend fun pushShops() {
        db.shopDao().getUnsyncedShops().forEach {
            runCatching { syncShop(it) }.onFailure { e -> Log.e(tag, "syncShop failed: ${e.message}") }
        }
    }

    /** Push only unsynced EMPLOYEE records. */
    suspend fun pushEmployees() {
        db.employeeDao().getUnsyncedEmployees().forEach {
            runCatching { syncEmployee(it) }.onFailure { e -> Log.e(tag, "syncEmployee failed: ${e.message}") }
        }
    }

    /** Push only unsynced INVESTOR records (with the invalid-id self-heal prep pass). */
    suspend fun pushInvestors() {
        // Ensure EVERY investor has a valid Firestore document id before pushing (a blank/
        // invalid id like one containing "/" would fail the write and block its children).
        db.investorDao().getAllInvestorsAsList().forEach { inv ->
            if (!isValidDocId(inv.investorId)) {
                Log.w(tag, "pushInvestors: investor id '${inv.investorId}' invalid — assigning a new id")
                db.investorDao().updateInvestor(
                    inv.copy(investorId = UUID.randomUUID().toString(), isSynced = false)
                )
            }
        }
        db.investorDao().getUnsyncedInvestors().forEach {
            runCatching { syncInvestor(it) }.onFailure { e -> Log.e(tag, "syncInvestor failed: ${e.message}") }
        }
    }

    /**
     * Push the WHOLE investor domain (investors + shop-investor links + transactions +
     * settlements + entries) — the connected unit the Investors screen syncs.
     */
    suspend fun pushInvestorDomain() {
        pushInvestors()
        db.shopInvestorDao().getUnsyncedShopInvestors().forEach {
            runCatching { syncShopInvestor(it) }.onFailure { e -> Log.e(tag, "syncShopInvestor failed: ${e.message}") }
        }
        db.investmentTransactionDao().getUnsyncedTransactions().forEach {
            runCatching { syncTransaction(it) }.onFailure { e -> Log.e(tag, "syncTransaction failed: ${e.message}") }
        }
        db.yearEndSettlementDao().getUnsyncedSettlements().forEach {
            runCatching { syncSettlement(it) }.onFailure { e -> Log.e(tag, "syncSettlement failed: ${e.message}") }
        }
        db.yearEndSettlementDao().getUnsyncedSettlementEntries().forEach {
            runCatching { syncSettlementEntry(it) }.onFailure { e -> Log.e(tag, "syncSettlementEntry failed: ${e.message}") }
        }
    }

    /**
     * A string is a usable Firestore document id only if it is non-blank, isn't "." or "..",
     * contains no "/", isn't a reserved __…__ id, and is ≤ 1500 bytes.
     */
    private fun isValidDocId(id: String): Boolean =
        id.isNotBlank() &&
            id != "." && id != ".." &&
            !id.contains("/") &&
            !Regex("__.*__").matches(id) &&
            id.toByteArray(Charsets.UTF_8).size <= 1500

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun firestoreSet(
        collection: String,
        docId: String,
        data: Map<String, Any?>
    ): Boolean = suspendCoroutine { cont ->
        // Guard against an invalid document id (e.g. one containing "/"), which makes
        // .document() throw SYNCHRONOUSLY — that would otherwise abort the whole syncAll
        // batch and silently drop this record (and everything after it).
        try {
            firestore.collection(collection)
                .document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(tag, "Synced $collection/$docId")
                    cont.resume(true)
                }
                .addOnFailureListener { e ->
                    Log.e(tag, "Failed to sync $collection/$docId: ${e.message}")
                    cont.resume(false)
                }
        } catch (e: Exception) {
            Log.e(tag, "Invalid write to $collection/'$docId': ${e.message}")
            cont.resume(false)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Deletes — propagate local deletions to Firestore so other devices drop them
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Pushes pending deletion tombstones: removes each main Firestore doc and writes a
     * `/deletions/{collection}_{id}` marker so other devices drop their local copy on pull.
     * Marked synced only on success → retried by the worker if offline.
     */
    suspend fun pushDeletions() {
        val pending = db.tombstoneDao().getUnsynced()
        if (pending.isEmpty()) return
        if (!ensureSignedIn()) return
        pending.forEach { t ->
            runCatching {
                firestoreDelete(t.collection, t.firebaseId)   // remove the main doc (idempotent)
                val markerOk = firestoreSet(
                    "deletions",
                    "${t.collection}_${t.firebaseId}",
                    mapOf(
                        "collection" to t.collection,
                        "firebaseId" to t.firebaseId,
                        "deletedAt"  to t.deletedAt
                    )
                )
                if (markerOk) db.tombstoneDao().markSynced(t.collection, t.firebaseId)
            }.onFailure { Log.e(tag, "pushDeletions failed for ${t.collection}/${t.firebaseId}: ${it.message}") }
        }
    }

    /** Deletes a shop document. No-op if its Firebase ID is blank. */
    suspend fun deleteShopDoc(shopFirebaseId: String): Boolean {
        if (shopFirebaseId.isBlank()) return true
        if (!ensureSignedIn()) return false
        return firestoreDelete("shops", shopFirebaseId)
    }

    /** Deletes a single transaction document. No-op if its Firebase ID is blank. */
    suspend fun deleteTransactionDoc(transactionFirebaseId: String): Boolean {
        if (transactionFirebaseId.isBlank()) return true
        if (!ensureSignedIn()) return false
        return firestoreDelete("transactions", transactionFirebaseId)
    }

    /**
     * Deletes an investor document and any of its (payment-free) shop-investor link docs.
     * Investor delete is only allowed when there are no payments, so there are no
     * transaction/settlement docs to cascade here.
     */
    suspend fun deleteInvestorWithLinks(
        investorFirebaseId: String,
        linkFirebaseIds: List<String>
    ): Boolean {
        if (!ensureSignedIn()) return false
        var ok = true
        linkFirebaseIds.filter { it.isNotBlank() }.forEach {
            if (!firestoreDelete("shop_investors", it)) ok = false
        }
        if (investorFirebaseId.isNotBlank()) {
            if (!firestoreDelete("investors", investorFirebaseId)) ok = false
        }
        return ok
    }

    /**
     * Deletes a settlement and all its entries from Firestore (explicit cascade —
     * Firestore has no FK cascade like Room does). Pass the entry IDs gathered from Room
     * BEFORE the local delete cascaded them away.
     */
    suspend fun deleteSettlementWithEntries(
        settlementFirebaseId: String,
        entryFirebaseIds: List<String>
    ): Boolean {
        if (!ensureSignedIn()) return false
        var ok = true
        entryFirebaseIds.filter { it.isNotBlank() }.forEach {
            if (!firestoreDelete("settlement_entries", it)) ok = false
        }
        if (settlementFirebaseId.isNotBlank()) {
            if (!firestoreDelete("settlements", settlementFirebaseId)) ok = false
        }
        return ok
    }

    private suspend fun firestoreDelete(collection: String, docId: String): Boolean =
        suspendCoroutine { cont ->
            firestore.collection(collection)
                .document(docId)
                .delete()
                .addOnSuccessListener {
                    Log.d(tag, "Deleted $collection/$docId")
                    cont.resume(true)
                }
                .addOnFailureListener { e ->
                    Log.e(tag, "Failed to delete $collection/$docId: ${e.message}")
                    cont.resume(false)
                }
        }
}
