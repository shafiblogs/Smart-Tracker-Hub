package com.marsa.smarttrackerhub.ui.screens.employees

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Created by Muhammed Shafi on 11/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

@Composable
fun EmployeesScreen(
    onAddClick: () -> Unit,
    onEditClick: (Int) -> Unit
) {
    val viewModel: EmployeeViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val shops by viewModel.shops.collectAsState()
    val showTerminated by viewModel.showTerminated.collectAsState()
    val context = LocalContext.current
    var showTerminateDialog by remember { mutableStateOf<Int?>(null) }
    var showReactivateDialog by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        viewModel.initDatabase(context)
        viewModel.loadEmployees()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Filter toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showTerminated) "All Employees" else "Active Employees",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                FilterChip(
                    selected = showTerminated,
                    onClick = { viewModel.toggleShowTerminated() },
                    label = {
                        Text(if (showTerminated) "Show Active Only" else "Show All")
                    }
                )
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.employees.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No employees found")
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.employees) { employee ->
                            val shopName =
                                shops.find { it.id == employee.associatedShopId }?.shopName
                                    ?: "Unknown Shop"

                            EmployeeCard(
                                employee = employee,
                                shopName = shopName,
                                onEditClick = { onEditClick(employee.id) },
                                onTerminateClick = { showTerminateDialog = employee.id },
                                onReactivateClick = { showReactivateDialog = employee.id }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Employee")
        }
    }

    // Terminate confirmation dialog
    showTerminateDialog?.let { employeeId ->
        AlertDialog(
            onDismissRequest = { showTerminateDialog = null },
            title = { Text("Terminate Employee") },
            text = { Text("Are you sure you want to terminate this employee? This action can be reversed later.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.terminateEmployee(
                            employeeId,
                            onSuccess = {
                                Toast.makeText(context, "Employee terminated", Toast.LENGTH_SHORT)
                                    .show()
                                showTerminateDialog = null
                            },
                            onFail = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Text("Terminate", color = Color(0xFFEF5350))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTerminateDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reactivate confirmation dialog
    showReactivateDialog?.let { employeeId ->
        AlertDialog(
            onDismissRequest = { showReactivateDialog = null },
            title = { Text("Reactivate Employee") },
            text = { Text("Are you sure you want to reactivate this employee?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.reactivateEmployee(
                            employeeId,
                            onSuccess = {
                                Toast.makeText(context, "Employee reactivated", Toast.LENGTH_SHORT)
                                    .show()
                                showReactivateDialog = null
                            },
                            onFail = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Text("Reactivate", color = Color(0xFF66BB6A))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReactivateDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}