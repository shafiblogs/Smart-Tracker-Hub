package com.marsa.smarttrackerhub.data.repository

import com.marsa.smarttrackerhub.data.dao.EmployeeInfoDao
import com.marsa.smarttrackerhub.data.entity.EmployeeInfo
import kotlinx.coroutines.flow.Flow


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
class EmployeeRepository(private val employeeDao: EmployeeInfoDao) {
    fun getActiveEmployees(): Flow<List<EmployeeInfo>> = employeeDao.getActiveEmployees()

    fun getTerminatedEmployees(): Flow<List<EmployeeInfo>> = employeeDao.getTerminatedEmployees()

    fun getAllEmployees(): Flow<List<EmployeeInfo>> = employeeDao.getAllEmployees()

    suspend fun getEmployeeById(id: Int): EmployeeInfo? = employeeDao.getEmployeeById(id)

    suspend fun terminateEmployee(id: Int) {
        employeeDao.terminateEmployee(id, System.currentTimeMillis())
    }

    suspend fun reactivateEmployee(id: Int) {
        employeeDao.reactivateEmployee(id)
    }

    suspend fun insertEmployee(employee: EmployeeInfo) = employeeDao.insertEmployee(employee)

    suspend fun updateEmployee(employee: EmployeeInfo) = employeeDao.updateEmployee(employee)

    suspend fun deleteEmployee(employee: EmployeeInfo) = employeeDao.deleteEmployee(employee)
}