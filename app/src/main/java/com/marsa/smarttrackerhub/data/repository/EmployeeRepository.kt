package com.marsa.smarttrackerhub.data.repository

import com.marsa.smarttrackerhub.data.dao.EmployeeInfoDao
import com.marsa.smarttrackerhub.data.entity.EmployeeInfo
import kotlinx.coroutines.flow.Flow


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class EmployeeRepository(private val dao: EmployeeInfoDao) {
    fun getAllEmployees(): Flow<List<EmployeeInfo>> = dao.getAllEmployees()
    fun getEmployeesByShop(shopId: Int): Flow<List<EmployeeInfo>> = dao.getEmployeesByShop(shopId)
    suspend fun insertEmployee(employee: EmployeeInfo) = dao.insertEmployee(employee)
    suspend fun updateEmployee(employee: EmployeeInfo) = dao.updateEmployee(employee)
    suspend fun deleteEmployee(employee: EmployeeInfo) = dao.deleteEmployee(employee)
}
