package com.marsa.smarttrackerhub.ui.screens.employees

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttrackerhub.ui.components.LabeledInputField


/**
 * Created by Muhammed Shafi on 11/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

@Composable
fun AddEmployeeScreen(
    onEmployeeCreated: () -> Unit
) {
    val viewModel: EmployeeViewModel = viewModel()
    val state by viewModel.formState.collectAsState()
    val isValid by viewModel.isFormValid.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initDatabase(context)
    }

    LaunchedEffect(isSaved) {
        if (isSaved) {
            onEmployeeCreated()
            viewModel.resetSaveState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LabeledInputField(
            label = "Employee Name",
            value = state.employeeName,
            onValueChange = viewModel::updateEmployeeName,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        LabeledInputField(
            label = "Email",
            value = state.employeeEmail,
            onValueChange = viewModel::updateEmployeeEmail,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        LabeledInputField(
            label = "Phone",
            value = state.employeePhone,
            onValueChange = viewModel::updateEmployeePhone,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        LabeledInputField(
            label = "Role",
            value = state.employeeRole,
            onValueChange = viewModel::updateEmployeeRole,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        LabeledInputField(
            label = "Salary",
            value = state.salary,
            onValueChange = viewModel::updateSalary,
            //keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        // You can add a dropdown to select associatedShopId from shops, if you want
        // For now, let's just input the ID manually:
        LabeledInputField(
            label = "Associated Shop ID",
            value = state.associatedShopId?.toString() ?: "",
            onValueChange = { input ->
                input.toIntOrNull()?.let { viewModel.updateAssociatedShopId(it) }
            },
            //keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        if (!error.isNullOrEmpty()) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                viewModel.saveEmployee(
                    onSuccess = {
                        Toast.makeText(context, "Employee saved", Toast.LENGTH_SHORT).show()
                    },
                    onFail = {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Save Employee")
        }
    }
}