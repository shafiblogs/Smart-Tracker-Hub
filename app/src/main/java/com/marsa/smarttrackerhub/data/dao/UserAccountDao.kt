package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.marsa.smarttrackerhub.data.entity.UserAccount


/**
 * Created by Muhammed Shafi on 18/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Dao
interface UserAccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: UserAccount)

    @Query("SELECT * FROM user_accounts WHERE userName = :userName AND password = :password LIMIT 1")
    suspend fun authenticate(userName: String, password: String): UserAccount?

    @Query("SELECT COUNT(*) FROM user_accounts")
    suspend fun hasAccounts(): Boolean

    @Query("SELECT * FROM user_accounts LIMIT 1")
    suspend fun getFirstAccount(): UserAccount?

    @Update
    suspend fun updateAccount(account: UserAccount)
}