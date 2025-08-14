package com.marsa.smarttrackerhub.data.dao


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Dao
interface EmployeeInfoDao {
    @Query("SELECT * FROM employee_info ORDER BY employeeName ASC")
    fun getAllEmployees(): Flow<List<EmployeeInfo>>

    @Query("SELECT * FROM employee_info WHERE associatedShopId = :shopId ORDER BY employeeName ASC")
    fun getEmployeesByShop(shopId: Int): Flow<List<EmployeeInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeInfo)

    @Update
    suspend fun updateEmployee(employee: EmployeeInfo)

    @Delete
    suspend fun deleteEmployee(employee: EmployeeInfo)
}
