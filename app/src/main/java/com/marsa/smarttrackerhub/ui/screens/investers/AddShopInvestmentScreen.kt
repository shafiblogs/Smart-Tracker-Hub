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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
 * Created by Muhammed Shafi on 18/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

/**
 * @param prefilledInvestorId  Pass > 0 when arriving from InvestorDetailScreen (investor is locked)
 * @param prefilledShopId      Pass > 0 when arriving from AddShopScreen (shop is locked)
 */
@Composable
fun AddShopInvestmentScreen(
    prefilledInvestorId: Int = 0,
    prefilledShopId: Int = 0,
    onSaveSuccess: () -> Unit
) {
    val viewModel: AddShopInvestmentViewModel = viewModel()
    val state by viewModel.formState.collectAsState()
    val shops by viewModel.shops.collectAsState()
    val investors by viewModel.investors.collectAsState()
    val isValid by viewModel.isFormValid.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val error by viewModel.error.collectAsState()
    val allocatedPercentage by viewModel.allocatedPercentage.collectAsState()
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val shopLocked = prefilledShopId > 0
    val investorLocked = prefilledInvestorId > 0

    LaunchedEffect(Unit) {
        viewModel.initDatabase(
            context = context,
            prefilledInvestorId = prefilledInvestorId,
            prefilledShopId = prefilledShopId
        )
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
                    text = "Investment Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Shop Dropdown (locked when shopLocked) ──
                DropdownField(
                    label = "Shop",
                    selectedValue = state.selectedShopName.ifBlank { "Select a shop" },
                    options = shops.map { it.shopName },
                    onOptionSelected = { selectedName ->
                        val shop = shops.find { it.shopName == selectedName }
                        shop?.let { viewModel.selectShop(it.id, it.shopName) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !shopLocked
                )

                if (state.shopError != null) {
                    Text(
                        text = state.shopError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                if (state.selectedShopId != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Allocated: ${String.format("%.1f", allocatedPercentage)}%  •  Remaining: ${String.format("%.1f", 100.0 - allocatedPercentage)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Investor Dropdown (locked when investorLocked) ──
                DropdownField(
                    label = "Investor",
                    selectedValue = state.selectedInvestorName.ifBlank { "Select an investor" },
                    options = investors.map { it.investorName },
                    onOptionSelected = { selectedName ->
                        val investor = investors.find { it.investorName == selectedName }
                        investor?.let { viewModel.selectInvestor(it.id, it.investorName) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !investorLocked
                )

                if (state.investorError != null) {
                    Text(
                        text = state.investorError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Share Percentage ──
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Share Percentage",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = state.sharePercentage,
                        onValueChange = viewModel::updateSharePercentage,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("e.g. 25.5") },
                        suffix = { Text("%") },
                        isError = state.shareError != null,
                        supportingText = if (state.shareError != null) {
                            { Text(state.shareError!!, color = MaterialTheme.colorScheme.error) }
                        } else null
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Investment Amount ──
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Investment Amount",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = state.investmentAmount,
                        onValueChange = viewModel::updateInvestmentAmount,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("Enter amount") },
                        prefix = { Text("AED ") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Investment Date ──
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Investment Date",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = dateFormat.format(Date(state.investmentDate)),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val calendar = Calendar.getInstance().apply {
                                        timeInMillis = state.investmentDate
                                    }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selected = Calendar.getInstance()
                                            selected.set(year, month, dayOfMonth, 0, 0, 0)
                                            selected.set(Calendar.MILLISECOND, 0)
                                            viewModel.updateInvestmentDate(selected.timeInMillis)
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                            }
                        }
                    )
                }

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

                Button(
                    onClick = {
                        viewModel.saveInvestment(
                            context = context,
                            onSuccess = {
                                Toast.makeText(context, "Investment added successfully!", Toast.LENGTH_SHORT).show()
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
                    Text(text = "Add Investment", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
