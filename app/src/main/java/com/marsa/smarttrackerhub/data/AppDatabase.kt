package com.marsa.smarttrackerhub.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.marsa.smarttrackerhub.data.dao.AccountSummaryDao
import com.marsa.smarttrackerhub.data.dao.EmployeeInfoDao
import com.marsa.smarttrackerhub.data.dao.InvestmentTransactionDao
import com.marsa.smarttrackerhub.data.dao.InvestorDao
import com.marsa.smarttrackerhub.data.dao.PurchaseDao
import com.marsa.smarttrackerhub.data.dao.ShopDao
import com.marsa.smarttrackerhub.data.dao.ShopInvestorDao
import com.marsa.smarttrackerhub.data.dao.SummaryDao
import com.marsa.smarttrackerhub.data.dao.TombstoneDao
import com.marsa.smarttrackerhub.data.dao.UserAccountDao
import com.marsa.smarttrackerhub.data.dao.YearEndSettlementDao
import com.marsa.smarttrackerhub.data.entity.AccountSummaryEntity
import com.marsa.smarttrackerhub.data.entity.EmployeeInfo
import com.marsa.smarttrackerhub.data.entity.InvestmentTransaction
import com.marsa.smarttrackerhub.data.entity.InvestorInfo
import com.marsa.smarttrackerhub.data.entity.PurchaseEntity
import com.marsa.smarttrackerhub.data.entity.SettlementEntry
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import com.marsa.smarttrackerhub.data.entity.ShopInvestor
import com.marsa.smarttrackerhub.data.entity.SummaryEntity
import com.marsa.smarttrackerhub.data.entity.Tombstone
import com.marsa.smarttrackerhub.data.entity.UserAccount
import com.marsa.smarttrackerhub.data.entity.YearEndSettlement
import com.marsa.smarttrackerhub.data.helper.Converters
import com.marsa.smarttrackerhub.data.migrations.MIGRATION_1_2
import com.marsa.smarttrackerhub.data.migrations.MIGRATION_2_3
import com.marsa.smarttrackerhub.data.migrations.MIGRATION_3_4
import com.marsa.smarttrackerhub.data.migrations.MIGRATION_4_5
import com.marsa.smarttrackerhub.data.migrations.MIGRATION_5_6
import com.marsa.smarttrackerhub.data.migrations.MIGRATION_6_7


/**
 * Created by Muhammed Shafi on 22/06/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 *
 * v2 — Investment module added:
 *   - Dropped EntryEntity (entries table)
 *   - Added ShopInvestor (junction: shop ↔ investor with share % and status)
 *   - Added InvestmentTransaction (phase-based payments per shop-investor)
 *   - Added YearEndSettlement (period-based reconciliation event per shop)
 *   - Added SettlementEntry (per-investor line inside a settlement)
 *
 * v3 — Consolidated production migration (shop sync, Firebase IDs, region):
 *   - shop_info: added shopStatus, totalInvested, isSynced, shopRegion
 *   - employee_info: added employeeId, associatedShopFirebaseId, isSynced
 *   - investor_info: added investorId, isSynced
 *   - shop_investor: added shopInvestorFirebaseId, isSynced
 *   - investment_transaction: added transactionFirebaseId, shopFirebaseId, investorFirebaseId, isSynced
 *   - year_end_settlement: added settlementFirebaseId, shopFirebaseId, isSynced
 *   - settlement_entry: added entryFirebaseId, investorFirebaseId, settlementFirebaseId, shopFirebaseId, isSynced
 *
 * v5 — Re-queue investor-domain rows stuck at isSynced=1 with blank Firebase IDs so they
 *   push to Firestore (data-fix only, no schema change).
 */

@Database(
    entities = [
        SummaryEntity::class,
        UserAccount::class,
        AccountSummaryEntity::class,
        ShopInfo::class,
        InvestorInfo::class,
        EmployeeInfo::class,
        ShopInvestor::class,
        InvestmentTransaction::class,
        YearEndSettlement::class,
        SettlementEntry::class,
        PurchaseEntity::class,
        Tombstone::class
    ],
    version = 7
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun summaryDao(): SummaryDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun shopDao(): ShopDao
    abstract fun investorDao(): InvestorDao
    abstract fun employeeDao(): EmployeeInfoDao
    abstract fun accountSummaryDao(): AccountSummaryDao
    abstract fun shopInvestorDao(): ShopInvestorDao
    abstract fun investmentTransactionDao(): InvestmentTransactionDao
    abstract fun yearEndSettlementDao(): YearEndSettlementDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun tombstoneDao(): TombstoneDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tracker_hub_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
