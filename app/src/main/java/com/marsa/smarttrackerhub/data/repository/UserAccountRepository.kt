package com.marsa.smarttrackerhub.data.repository

import com.marsa.smarttrackerhub.data.dao.UserAccountDao
import com.marsa.smarttrackerhub.data.entity.UserAccount


/**
 * Created by Muhammed Shafi on 18/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class UserAccountRepository(private val dao: UserAccountDao) {
    suspend fun authenticate(userName: String, password: String): UserAccount? =
        dao.authenticate(userName, password)

    suspend fun updateAccount(account: UserAccount) =
        dao.updateAccount(account)

    suspend fun insertAccount(account: UserAccount) =
        dao.insert(account)

    suspend fun hasAnyAccount(): Boolean = dao.hasAccounts()

    suspend fun getFirstAccount(): UserAccount? = dao.getFirstAccount()
}
