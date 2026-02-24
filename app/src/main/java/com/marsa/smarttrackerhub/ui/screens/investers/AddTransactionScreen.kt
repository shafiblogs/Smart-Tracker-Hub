package com.marsa.smarttrackerhub.ui.screens.investers

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttrackerhub.ui.components.DropdownField
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Records a phase payment by an investor to a shop.
 *
 * @param shopId              The shop this transaction belongs to
 * @param prefilledInvestorId > 0 → investor picker is pre-selected and locked
 *
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    shopId: Int,
    prefilledInvestorId: Int = 0,
    onSaveSuccess: () -> Unit
) {
    val viewModel: AddTransactionViewModel = viewModel()
    val state by viewModel.formState.collectAsState()
    val shopInvestors by viewModel.shopInvestors.collectAsState()
    val existingPhases by viewModel.existingPhases.collectAsState()
    val isValid by viewModel.isFormValid.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    val investorLocked = prefilledInvestorId > 0
    var phaseMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.initDatabase(context, shopId, prefilledInvestorId)
    }
    LaunchedEffect(isSaved) {
        if (isSaved) onSaveSuccess()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
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
                Text(
                    text = "Record Payment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Record how much an investor contributed in this phase.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Investor Picker ──
                DropdownField(
                    label = "Investor",
                    selectedValue = state.selectedInvestorName.ifBlank { "Select investor" },
                    options = shopInvestors.map { it.second.investorName },
                    onOptionSelected = { name ->
                        shopInvestors.find { it.second.investorName == name }
                            ?.let { (si, inv) -> viewModel.selectInvestor(si.id, inv.investorName) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !investorLocked
                )
                if (state.investorError != null) {
                    Text(state.investorError!!, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Phase Label (with suggestions) ──
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Phase",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = phaseMenuExpanded && existingPhases.isNotEmpty(),
                        onExpandedChange = { phaseMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = state.phase,
                            onValueChange = {
                                viewModel.updatePhase(it)
                                phaseMenuExpanded = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            placeholder = { Text("e.g. Phase 1, Initial Setup") },
                            trailingIcon = {
                                if (existingPhases.isNotEmpty())
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = phaseMenuExpanded)
                            },
                            isError = state.phaseError != null,
                            supportingText = if (state.phaseError != null) {
                                { Text(state.phaseError!!, color = MaterialTheme.colorScheme.error) }
                            } else null,
                            singleLine = true
                        )
                        if (existingPhases.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = phaseMenuExpanded,
                                onDismissRequest = { phaseMenuExpanded = false }
                            ) {
                                existingPhases.forEach { phase ->
                                    DropdownMenuItem(
                                        text = { Text(phase) },
                                        onClick = {
                                            viewModel.updatePhase(phase)
                                            phaseMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Amount ──
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = state.amount,
                        onValueChange = viewModel::updateAmount,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("Enter amount") },
                        prefix = { Text("AED ") },
                        isError = state.amountError != null,
                        supportingText = if (state.amountError != null) {
                            { Text(state.amountError!!, color = MaterialTheme.colorScheme.error) }
                        } else null
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Date ──
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Payment Date",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = dateFormat.format(Date(state.transactionDate)),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = state.transactionDate }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        Calendar.getInstance().also {
                                            it.set(y, m, d, 0, 0, 0)
                                            it.set(Calendar.MILLISECOND, 0)
                                            viewModel.updateDate(it.timeInMillis)
                                        }
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Pick date")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Note (optional) ──
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Note (optional)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = viewModel::updateNote,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Bank transfer ref #12345") },
                        minLines = 2,
                        maxLines = 3
                    )
                }

                if (!error.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        viewModel.saveTransaction(
                            context = context,
                            onSuccess = {
                                Toast.makeText(context, "Payment recorded!", Toast.LENGTH_SHORT).show()
                            },
                            onFail = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    enabled = isValid,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Record Payment", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
