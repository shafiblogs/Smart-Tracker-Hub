package com.marsa.smarttrackerhub.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.marsa.smarttrackerhub.data.dao.CategoryDao
import com.marsa.smarttrackerhub.data.dao.EntryDao
import com.marsa.smarttrackerhub.data.dao.SalesDao
import com.marsa.smarttrackerhub.data.dao.ShopDao
import com.marsa.smarttrackerhub.data.dao.SummaryDao
import com.marsa.smarttrackerhub.data.dao.UserAccountDao
import com.marsa.smarttrackerhub.data.dao.VendorDao
import com.marsa.smarttrackerhub.data.entity.Category
import com.marsa.smarttrackerhub.data.entity.EntryEntity
import com.marsa.smarttrackerhub.data.entity.SaleEntity
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import com.marsa.smarttrackerhub.data.entity.SummaryEntity
import com.marsa.smarttrackerhub.data.entity.UserAccount
import com.marsa.smarttrackerhub.data.entity.Vendor
import com.marsa.smarttrackerhub.data.helper.Converters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.marsa.smarttrackerhub.ui.screens.enums.ScreenType


/**
 * Created by Muhammed Shafi on 22/06/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

@Database(
    entities = [EntryEntity::class, SummaryEntity::class, SaleEntity::class, UserAccount::class, Category::class, ShopInfo::class, Vendor::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun summaryDao(): SummaryDao
    abstract fun salesDao(): SalesDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun vendorDao(): VendorDao
    abstract fun shopDao(): ShopDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "entry_hub_db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default categories on first creation
                        CoroutineScope(Dispatchers.IO).launch {
                            getDatabase(context).categoryDao().insertAll(
                                listOf(
                                    Category(
                                        name = "Fruits & Vegetables",
                                        description = "market purchase",
                                        screenType = ScreenType.Purchase.name
                                    ),
                                    Category(
                                        name = "Milk & Dairy",
                                        description = "diary products",
                                        screenType = ScreenType.Purchase.name
                                    ),
                                    Category(
                                        name = "Bakery",
                                        description = "bakery products",
                                        screenType = ScreenType.Purchase.name
                                    ),
                                    Category(
                                        name = "Rent",
                                        description = "rent",
                                        screenType = ScreenType.Expense.name
                                    ),
                                    Category(
                                        name = "Utilities",
                                        description = "electricity and water",
                                        screenType = ScreenType.Expense.name
                                    ),
                                    Category(
                                        name = "Bank Charges",
                                        description = "bank charges",
                                        screenType = ScreenType.Expense.name
                                    ),
                                )
                            )
                        }
                    }
                }).build().also { INSTANCE = it }
            }
        }
    }
}