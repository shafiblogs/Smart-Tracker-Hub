package com.marsa.smarttrackerhub.ui.screens.employees

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.EmployeeInfo
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import com.marsa.smarttrackerhub.data.repository.EmployeeRepository
import com.marsa.smarttrackerhub.data.repository.ShopRepository
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

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var editingEmployeeId: Int? = null

    private lateinit var employeeRepository: EmployeeRepository
    private lateinit var shopRepository: ShopRepository

    private val _shops = MutableStateFlow<List<ShopInfo>>(emptyList())
    val shops: StateFlow<List<ShopInfo>> = _shops.asStateFlow()

    private val _uiState = MutableStateFlow(EmployeeListUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _showTerminated = MutableStateFlow(false)
    val showTerminated = _showTerminated.asStateFlow()

    fun toggleShowTerminated() {
        _showTerminated.value = !_showTerminated.value
        loadEmployees()
    }

    fun loadEmployees() = viewModelScope.launch {
        val flow = if (_showTerminated.value) {
            employeeRepository.getAllEmployees()
        } else {
            employeeRepository.getActiveEmployees()
        }

        flow.collect { employees ->
            _uiState.value = EmployeeListUiState(employees = employees, isLoading = false)
        }
    }

    fun terminateEmployee(
        employeeId: Int,
        onSuccess: () -> Unit = {},
        onFail: (String) -> Unit = {}
    ) =
        viewModelScope.launch {
            try {
                employeeRepository.terminateEmployee(employeeId)
                onSuccess()
            } catch (e: Exception) {
                onFail("Failed to terminate employee: ${e.localizedMessage}")
            }
        }

    fun initDatabase(context: Context) {
        val db = AppDatabase.getDatabase(context)
        employeeRepository = EmployeeRepository(db.employeeDao())
        shopRepository = ShopRepository(db.shopDao())
        loadShops()
    }

    private fun loadShops() = viewModelScope.launch {
        shopRepository.getAllShops().collect { shopList ->
            _shops.value = shopList
        }
    }

    val isFormValid: StateFlow<Boolean> = formState
        .map {
            it.employeeName.isNotBlank() &&
                    it.employeePhone.isNotBlank() &&
                    it.employeeRole != null &&
                    it.salary.toDoubleOrNull() != null &&
                    it.allowance.toDoubleOrNull() != null &&
                    it.associatedShopId != null &&
                    it.visaExpiryDate != null
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun loadEmployee(employeeId: Int) = viewModelScope.launch {
        try {
            val employee = employeeRepository.getEmployeeById(employeeId)
            employee?.let {
                editingEmployeeId = it.id
                _formState.value = EmployeeFormState(
                    employeeName = it.employeeName,
                    employeePhone = it.employeePhone,
                    employeeRole = EmployeeRole.valueOf(it.employeeRole),
                    salary = it.salary.toString(),
                    allowance = it.allowance.toString(),
                    associatedShopId = it.associatedShopId,
                    visaExpiryDate = it.visaExpiryDate
                )
                _isLoaded.value = true
            }
        } catch (e: Exception) {
            _error.value = "Failed to load employee: ${e.localizedMessage}"
        }
    }

    fun updateEmployeeName(name: String) {
        _formState.update { it.copy(employeeName = name) }
        _error.value = null
    }

    fun updateEmployeePhone(phone: String) {
        _formState.update { it.copy(employeePhone = phone) }
        _error.value = null
    }

    fun updateEmployeeRole(role: EmployeeRole) {
        _formState.update { it.copy(employeeRole = role) }
        _error.value = null
    }

    fun updateSalary(salary: String) {
        if (salary.isEmpty() || salary.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(salary = salary) }
            _error.value = null
        }
    }

    fun updateAllowance(allowance: String) {
        if (allowance.isEmpty() || allowance.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(allowance = allowance) }
            _error.value = null
        }
    }

    fun updateAssociatedShopId(shopId: Int) {
        _formState.update { it.copy(associatedShopId = shopId) }
        _error.value = null
    }

    fun updateVisaExpiryDate(dateInMillis: Long) {
        _formState.update { it.copy(visaExpiryDate = dateInMillis) }
        _error.value = null
    }

    fun saveEmployee(onSuccess: () -> Unit = {}, onFail: (String) -> Unit = {}) =
        viewModelScope.launch {
            val state = _formState.value
            try {
                val salaryDouble = state.salary.toDoubleOrNull()
                val allowanceDouble = state.allowance.toDoubleOrNull()

                if (salaryDouble == null) {
                    onFail("Invalid salary format")
                    return@launch
                }
                if (allowanceDouble == null) {
                    onFail("Invalid allowance format")
                    return@launch
                }

                val employee = EmployeeInfo(
                    id = editingEmployeeId ?: 0,
                    employeeName = state.employeeName,
                    employeePhone = state.employeePhone,
                    employeeRole = state.employeeRole!!.name,
                    salary = salaryDouble,
                    allowance = allowanceDouble,
                    associatedShopId = state.associatedShopId!!,
                    visaExpiryDate = state.visaExpiryDate ?: 0L
                )

                if (editingEmployeeId != null) {
                    employeeRepository.updateEmployee(employee)
                } else {
                    employeeRepository.insertEmployee(employee)
                }
                _isSaved.value = true
                onSuccess()
            } catch (e: Exception) {
                onFail("Failed to save employee: ${e.localizedMessage}")
            }
        }

    fun reactivateEmployee(
        employeeId: Int,
        onSuccess: () -> Unit = {},
        onFail: (String) -> Unit = {}
    ) =
        viewModelScope.launch {
            try {
                employeeRepository.reactivateEmployee(employeeId)
                onSuccess()
            } catch (e: Exception) {
                onFail("Failed to reactivate employee: ${e.localizedMessage}")
            }
        }

    fun resetSaveState() {
        _isSaved.value = false
    }
}