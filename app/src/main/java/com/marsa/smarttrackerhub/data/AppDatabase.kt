package com.marsa.smarttrackerhub.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.marsa.smarttrackerhub.data.dao.AccountSummaryDao
import com.marsa.smarttrackerhub.data.dao.EmployeeInfoDao
import com.marsa.smarttrackerhub.data.dao.EntryDao
import com.marsa.smarttrackerhub.data.dao.InvestorDao
import com.marsa.smarttrackerhub.data.dao.ShopDao
import com.marsa.smarttrackerhub.data.dao.SummaryDao
import com.marsa.smarttrackerhub.data.dao.UserAccountDao
import com.marsa.smarttrackerhub.data.entity.AccountSummaryEntity
import com.marsa.smarttrackerhub.data.entity.EmployeeInfo
import com.marsa.smarttrackerhub.data.entity.EntryEntity
import com.marsa.smarttrackerhub.data.entity.InvestorInfo
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import com.marsa.smarttrackerhub.data.entity.SummaryEntity
import com.marsa.smarttrackerhub.data.entity.UserAccount
import com.marsa.smarttrackerhub.data.helper.Converters


/**
 * Created by Muhammed Shafi on 22/06/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

@Database(
    entities = [EntryEntity::class, SummaryEntity::class, UserAccount::class, AccountSummaryEntity::class,
        ShopInfo::class, InvestorInfo::class, EmployeeInfo::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun summaryDao(): SummaryDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun shopDao(): ShopDao
    abstract fun investorDao(): InvestorDao
    abstract fun employeeDao(): EmployeeInfoDao
    abstract fun accountSummaryDao(): AccountSummaryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "tracker_hub_db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                    }
                }).build().also { INSTANCE = it }
            }
        }
    }
}