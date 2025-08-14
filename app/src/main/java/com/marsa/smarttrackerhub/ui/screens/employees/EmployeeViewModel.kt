package com.marsa.smarttrackerhub.ui.screens.employees

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.EmployeeInfo
import com.marsa.smarttrackerhub.data.repository.EmployeeRepository
import com.marsa.smarttrackerhub.data.repository.InvestorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

class EmployeeViewModel : ViewModel() {

    private val _formState = MutableStateFlow(EmployeeFormState())
    val formState = _formState.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved = _isSaved.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var editingEmployeeId: Int? = null

    private lateinit var repository: EmployeeRepository
    fun initDatabase(context: Context) {
        val db = AppDatabase.getDatabase(context)
        repository = EmployeeRepository(db.employeeDao())
    }

    val isFormValid: StateFlow<Boolean> = formState
        .map {
            it.employeeName.isNotBlank() &&
                    it.employeeEmail.isNotBlank() &&
                    it.employeePhone.isNotBlank() &&
                    it.employeeRole.isNotBlank() &&
                    it.salary.toDoubleOrNull() != null &&
                    it.associatedShopId != null
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun updateEmployeeName(name: String) = _formState.update { it.copy(employeeName = name) }
    fun updateEmployeeEmail(email: String) = _formState.update { it.copy(employeeEmail = email) }
    fun updateEmployeePhone(phone: String) = _formState.update { it.copy(employeePhone = phone) }
    fun updateEmployeeRole(role: String) = _formState.update { it.copy(employeeRole = role) }
    fun updateSalary(salary: String) = _formState.update { it.copy(salary = salary) }
    fun updateAssociatedShopId(shopId: Int) =
        _formState.update { it.copy(associatedShopId = shopId) }

    fun saveEmployee(onSuccess: () -> Unit = {}, onFail: (String) -> Unit = {}) =
        viewModelScope.launch {
            val state = _formState.value
            try {
                val salaryDouble = state.salary.toDoubleOrNull()
                if (salaryDouble == null) {
                    onFail("Invalid salary format")
                    return@launch
                }
                val employee = EmployeeInfo(
                    id = editingEmployeeId ?: 0,
                    employeeName = state.employeeName,
                    employeeEmail = state.employeeEmail,
                    employeePhone = state.employeePhone,
                    employeeRole = state.employeeRole,
                    salary = salaryDouble,
                    associatedShopId = state.associatedShopId!!
                )
                if (editingEmployeeId != null) {
                    repository.updateEmployee(employee)
                } else {
                    repository.insertEmployee(employee)
                }
                _isSaved.value = true
                onSuccess()
            } catch (e: Exception) {
                onFail("Failed to save employee: ${e.localizedMessage}")
            }
        }

    private val _uiState = MutableStateFlow(EmployeeListUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    fun loadEmployees() = viewModelScope.launch {
        repository.getAllEmployees().collect { employees ->
            _uiState.value = EmployeeListUiState(employees = employees, isLoading = false)
        }
    }

    fun resetSaveState() {
        _isSaved.value = false
    }
}