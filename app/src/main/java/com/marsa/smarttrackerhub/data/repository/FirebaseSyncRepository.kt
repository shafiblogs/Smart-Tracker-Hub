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
        if (auth.currentUser != null) return true
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

    suspend fun syncShop(entity: ShopInfo): Boolean {
        // Records with blank shopId are pre-v6 or invalid — mark as "handled" to stop retry loop
        if (entity.shopId.isBlank()) {
            db.shopDao().markShopSynced("")
            return true
        }
        if (!ensureSignedIn()) return false

        val map = mapOf(
            "shopId"           to entity.shopId,
            "shopName"         to entity.shopName,
            "shopAddress"      to entity.shopAddress,
            "shopType"         to entity.shopType,
            "shopStatus"       to entity.shopStatus,
            "zakathStatus"     to entity.zakathStatus,
            "licenseExpiryDate" to entity.licenseExpiryDate,
            "shopOpeningDate"  to entity.shopOpeningDate,
            "stockValue"       to entity.stockValue,
            "stockTakenDate"   to entity.stockTakenDate,
            "totalInvested"    to entity.totalInvested,
            "shopRegion"       to entity.shopRegion
        )

        val success = firestoreSet("shops", entity.shopId, map)
        if (success) db.shopDao().markShopSynced(entity.shopId)
        return success
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Investor
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun syncInvestor(entity: InvestorInfo): Boolean {
        if (entity.investorId.isBlank()) {
            db.investorDao().markInvestorSynced("")
            return true
        }
        if (!ensureSignedIn()) return false

        val map = mapOf(
            "investorId"    to entity.investorId,
            "investorName"  to entity.investorName,
            "investorEmail" to entity.investorEmail,
            "investorPhone" to entity.investorPhone
        )

        val success = firestoreSet("investors", entity.investorId, map)
        if (success) db.investorDao().markInvestorSynced(entity.investorId)
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
            "isActive"                 to resolvedEntity.isActive
        )

        val success = firestoreSet("employees", resolvedEntity.employeeId, map)
        if (success) db.employeeDao().markEmployeeSynced(resolvedEntity.employeeId)
        return success
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ShopInvestor
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun syncShopInvestor(entity: ShopInvestor): Boolean {
        if (entity.shopInvestorFirebaseId.isBlank()) {
            db.shopInvestorDao().markShopInvestorSynced("")
            return true
        }
        if (!ensureSignedIn()) return false

        // Resolve Firebase string IDs from Room local-int references
        val shopFirebaseId     = db.shopDao().getShopById(entity.shopId)?.shopId ?: ""
        val investorFirebaseId = db.investorDao().getInvestorById(entity.investorId)?.investorId ?: ""

        val map = mapOf(
            "shopInvestorFirebaseId" to entity.shopInvestorFirebaseId,
            "shopFirebaseId"         to shopFirebaseId,
            "investorFirebaseId"     to investorFirebaseId,
            "sharePercentage"        to entity.sharePercentage,
            "status"                 to entity.status,
            "joinedDate"             to entity.joinedDate
        )

        val success = firestoreSet("shop_investors", entity.shopInvestorFirebaseId, map)
        if (success) db.shopInvestorDao().markShopInvestorSynced(entity.shopInvestorFirebaseId)
        return success
    }

    // ─────────────────────────────────────────────────────────────────────────
    // InvestmentTransaction
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun syncTransaction(entity: InvestmentTransaction): Boolean {
        if (entity.transactionFirebaseId.isBlank()) {
            db.investmentTransactionDao().markTransactionSynced("")
            return true
        }
        if (!ensureSignedIn()) return false

        val map = mapOf(
            "transactionFirebaseId" to entity.transactionFirebaseId,
            "shopFirebaseId"        to entity.shopFirebaseId,
            "investorFirebaseId"    to entity.investorFirebaseId,
            "amount"                to entity.amount,
            "transactionDate"       to entity.transactionDate,
            "phase"                 to entity.phase,
            "note"                  to entity.note
        )

        val success = firestoreSet("transactions", entity.transactionFirebaseId, map)
        if (success) db.investmentTransactionDao().markTransactionSynced(entity.transactionFirebaseId)
        return success
    }

    // ─────────────────────────────────────────────────────────────────────────
    // YearEndSettlement
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun syncSettlement(entity: YearEndSettlement): Boolean {
        if (entity.settlementFirebaseId.isBlank()) {
            db.yearEndSettlementDao().markSettlementSynced("")
            return true
        }
        if (!ensureSignedIn()) return false

        val map = mapOf(
            "settlementFirebaseId" to entity.settlementFirebaseId,
            "shopFirebaseId"       to entity.shopFirebaseId,
            "settlementDate"       to entity.settlementDate,
            "periodStartDate"      to entity.periodStartDate,
            "totalInvested"        to entity.totalInvested,
            "note"                 to entity.note,
            "isCarriedForward"     to entity.isCarriedForward
        )

        val success = firestoreSet("settlements", entity.settlementFirebaseId, map)
        if (success) db.yearEndSettlementDao().markSettlementSynced(entity.settlementFirebaseId)
        return success
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SettlementEntry
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun syncSettlementEntry(entity: SettlementEntry): Boolean {
        if (entity.entryFirebaseId.isBlank()) {
            db.yearEndSettlementDao().markSettlementEntrySynced("")
            return true
        }
        if (!ensureSignedIn()) return false

        val map = mapOf(
            "entryFirebaseId"      to entity.entryFirebaseId,
            "settlementFirebaseId" to entity.settlementFirebaseId,
            "shopFirebaseId"       to entity.shopFirebaseId,
            "investorFirebaseId"   to entity.investorFirebaseId,
            "fairShareAmount"      to entity.fairShareAmount,
            "actualPaidAmount"     to entity.actualPaidAmount,
            "balanceAmount"        to entity.balanceAmount,
            "settlementPaidAmount" to entity.settlementPaidAmount,
            "settlementPaidDate"   to entity.settlementPaidDate
        )

        val success = firestoreSet("settlement_entries", entity.entryFirebaseId, map)
        if (success) db.yearEndSettlementDao().markSettlementEntrySynced(entity.entryFirebaseId)
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
        // Step 1: Collect all pending records from Room (fast, local, no network)
        val shops       = db.shopDao().getUnsyncedShops()
        val investors   = db.investorDao().getUnsyncedInvestors()
        val employees   = db.employeeDao().getUnsyncedEmployees()
        val shopInvs    = db.shopInvestorDao().getUnsyncedShopInvestors()
        val txns        = db.investmentTransactionDao().getUnsyncedTransactions()
        val settlements = db.yearEndSettlementDao().getUnsyncedSettlements()
        val entries     = db.yearEndSettlementDao().getUnsyncedSettlementEntries()

        // Step 2: Nothing pending — skip Firebase entirely (no auth, no connection)
        val totalPending = shops.size + investors.size + employees.size +
                shopInvs.size + txns.size + settlements.size + entries.size
        if (totalPending == 0) {
            Log.d(tag, "syncAll() — nothing pending, skipping Firebase connection")
            return
        }

        // Step 3: Something to push — now connect and push in dependency order
        Log.d(tag, "syncAll() — $totalPending record(s) pending, connecting to Firebase")
        shops.forEach { syncShop(it) }
        investors.forEach { syncInvestor(it) }
        employees.forEach { syncEmployee(it) }
        shopInvs.forEach { syncShopInvestor(it) }
        txns.forEach { syncTransaction(it) }
        settlements.forEach { syncSettlement(it) }
        entries.forEach { syncSettlementEntry(it) }
        Log.d(tag, "syncAll() completed")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun firestoreSet(
        collection: String,
        docId: String,
        data: Map<String, Any?>
    ): Boolean = suspendCoroutine { cont ->
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
    }
}
