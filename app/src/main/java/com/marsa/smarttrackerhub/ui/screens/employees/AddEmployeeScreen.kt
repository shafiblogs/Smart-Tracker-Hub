package com.marsa.smarttrackerhub.ui.screens.employees

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttrackerhub.ui.components.DropdownField
import com.marsa.smarttrackerhub.ui.components.LabeledInputField
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Created by Muhammed Shafi on 11/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmployeeScreen(
    employeeId: Int? = null,
    onEmployeeCreated: () -> Unit
) {
    val viewModel: EmployeeViewModel = viewModel()
    val state by viewModel.formState.collectAsState()
    val isValid by viewModel.isFormValid.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val error by viewModel.error.collectAsState()
    val shops by viewModel.shops.collectAsState()
    val isLoaded by viewModel.isLoaded.collectAsState()
    val context = LocalContext.current

    // Edit mode state - disabled by default when loading existing employee
    var isEditEnabled by remember { mutableStateOf(employeeId == null) }

    LaunchedEffect(Unit) {
        viewModel.initDatabase(context)
    }

    LaunchedEffect(employeeId) {
        employeeId?.let {
            viewModel.loadEmployee(it)
            isEditEnabled = false // Disable edit mode when loading
        }
    }

    LaunchedEffect(isSaved) {
        if (isSaved) {
            onEmployeeCreated()
            viewModel.resetSaveState()
        }
    }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            // Show edit button only when in view mode (existing employee)
            if (isLoaded && !isEditEnabled) {
                FloatingActionButton(
                    onClick = { isEditEnabled = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Enable Edit"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {

                // ============ EMPLOYEE INFORMATION SECTION ============
                SectionHeader(text = "Employee Information")

                Spacer(modifier = Modifier.height(16.dp))

                LabeledInputField(
                    label = "Employee Name",
                    value = state.employeeName,
                    maxLength = 30,
                    onValueChange = viewModel::updateEmployeeName,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditEnabled
                )

                Spacer(modifier = Modifier.height(16.dp))

                LabeledInputField(
                    label = "Phone Number",
                    value = state.employeePhone,
                    maxLength = 15,
                    onValueChange = viewModel::updateEmployeePhone,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditEnabled
                )

                Spacer(modifier = Modifier.height(16.dp))

                DropdownField(
                    label = "Employee Role",
                    selectedValue = state.employeeRole?.displayName() ?: "Select Role",
                    options = EmployeeRole.values().map { it.displayName() },
                    onOptionSelected = { selected ->
                        val role = EmployeeRole.values().find { it.displayName() == selected }
                        role?.let { viewModel.updateEmployeeRole(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditEnabled
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Associated Shop Dropdown
                DropdownField(
                    label = "Associated Shop",
                    selectedValue = shops.find { it.id == state.associatedShopId }?.shopName
                        ?: "Select Shop",
                    options = shops.map { it.shopName },
                    onOptionSelected = { selected ->
                        val shop = shops.find { it.shopName == selected }
                        shop?.let { viewModel.updateAssociatedShopId(it.id) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditEnabled
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Visa Expiry Date
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Visa Expiry Date",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = state.visaExpiryDate?.let {
                            dateFormat.format(Date(it))
                        } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = isEditEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (isEditEnabled) {
                                IconButton(
                                    onClick = {
                                        val calendar = Calendar.getInstance()
                                        state.visaExpiryDate?.let {
                                            calendar.timeInMillis = it
                                        }

                                        DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                val selectedCalendar = Calendar.getInstance()
                                                selectedCalendar.set(
                                                    year,
                                                    month,
                                                    dayOfMonth,
                                                    0,
                                                    0,
                                                    0
                                                )
                                                selectedCalendar.set(Calendar.MILLISECOND, 0)
                                                viewModel.updateVisaExpiryDate(selectedCalendar.timeInMillis)
                                            },
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH),
                                            calendar.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Select Date"
                                    )
                                }
                            }
                        },
                        placeholder = { Text("Select visa expiry date") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(24.dp))

                // ============ SALARY INFORMATION SECTION ============
                SectionHeader(text = "Salary Information")

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Basic Salary",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = state.salary,
                        onValueChange = viewModel::updateSalary,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("Enter salary") },
                        prefix = { Text("AED ") },
                        enabled = isEditEnabled
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Allowance",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = state.allowance,
                        onValueChange = viewModel::updateAllowance,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("Enter allowance") },
                        prefix = { Text("AED ") },
                        enabled = isEditEnabled
                    )
                }

                // Error Display
                if (!error.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Submit Button - only show when edit is enabled
                if (isEditEnabled) {
                    val buttonText = if (isLoaded) "Save Changes" else "Create Employee"
                    Button(
                        onClick = {
                            viewModel.saveEmployee(
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "Employee saved successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onFail = { errorMessage ->
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        enabled = isValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = buttonText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}