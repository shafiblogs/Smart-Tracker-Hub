package com.marsa.smarttrackerhub.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.marsa.smarttrackerhub.data.entity.EmployeeInfo
import kotlinx.coroutines.flow.Flow

/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Dao
interface EmployeeInfoDao {
    @Query("SELECT * FROM employee_info WHERE isActive = 1")
    fun getActiveEmployees(): Flow<List<EmployeeInfo>>

    @Query("SELECT * FROM employee_info WHERE isActive = 0")
    fun getTerminatedEmployees(): Flow<List<EmployeeInfo>>

    @Query("SELECT * FROM employee_info")
    fun getAllEmployees(): Flow<List<EmployeeInfo>>

    @Query("SELECT * FROM employee_info WHERE id = :id")
    suspend fun getEmployeeById(id: Int): EmployeeInfo?

    @Query("UPDATE employee_info SET isActive = 0, terminationDate = :terminationDate WHERE id = :id")
    suspend fun terminateEmployee(id: Int, terminationDate: Long)

    @Query("UPDATE employee_info SET isActive = 1, terminationDate = NULL WHERE id = :id")
    suspend fun reactivateEmployee(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeInfo)

    @Update
    suspend fun updateEmployee(employee: EmployeeInfo)

    @Delete
    suspend fun deleteEmployee(employee: EmployeeInfo)

    /** Returns the count of employees that already use [employeeId], excluding [excludeId].
     *  Pass excludeId = 0 for new inserts. */
    @Query("SELECT COUNT(*) FROM employee_info WHERE employeeId = :employeeId AND id != :excludeId")
    suspend fun countByEmployeeId(employeeId: String, excludeId: Int): Int

    // ── Firebase sync ──────────────────────────────────────────────────────────

    /** All employees not yet pushed to Firestore. */
    @Query("SELECT * FROM employee_info WHERE isSynced = 0")
    suspend fun getUnsyncedEmployees(): List<EmployeeInfo>

    /** Marks the employee with the given [employeeId] string as synced. */
    @Query("UPDATE employee_info SET isSynced = 1 WHERE employeeId = :employeeId")
    suspend fun markEmployeeSynced(employeeId: String)
}
