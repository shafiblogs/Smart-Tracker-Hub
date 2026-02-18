package com.marsa.smarttrackerhub.ui.screens.shops

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.marsa.smarttrackerhub.domain.ShopInvestorSummary
import com.marsa.smarttrackerhub.ui.components.DropdownField
import com.marsa.smarttrackerhub.ui.components.LabeledInputField
import com.marsa.smarttrackerhub.ui.screens.enums.ShopType
import com.marsa.smarttrackerhub.utils.HijriDateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShopScreen(
    shopId: Int? = null,
    onShopCreated: () -> Unit,
    onAddInvestorClick: (Int) -> Unit = {}
) {
    val viewModel: AddShopViewModel = viewModel()
    val state by viewModel.formState.collectAsState()
    val isValid by viewModel.isFormValid.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val error by viewModel.error.collectAsState()
    val zakathAmount by viewModel.zakathAmount.collectAsState()
    val shopInvestors by viewModel.shopInvestors.collectAsState()
    val context = LocalContext.current
    val isLoaded by viewModel.isLoaded.collectAsState()

    // Edit mode state - disabled by default when loading existing shop
    var isEditEnabled by remember { mutableStateOf(shopId == null) }

    LaunchedEffect(shopId) {
        shopId?.let {
            viewModel.loadShop(context, it)
            isEditEnabled = false // Disable edit mode when loading
        }
    }

    LaunchedEffect(isSaved) {
        if (isSaved) {
            onShopCreated()
        }
    }

    val gregorianDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            // Show edit button only when in view mode (existing shop)
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

                // ============ SHOP INFORMATION SECTION ============
                SectionHeader(text = "Shop Information")

                Spacer(modifier = Modifier.height(16.dp))

                LabeledInputField(
                    label = "Shop Name",
                    value = state.shopName,
                    maxLength = 20,
                    onValueChange = viewModel::updateShopName,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditEnabled
                )

                Spacer(modifier = Modifier.height(16.dp))

                LabeledInputField(
                    label = "Address",
                    value = state.shopAddress,
                    maxLength = 30,
                    onValueChange = viewModel::updateShopAddress,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditEnabled
                )

                Spacer(modifier = Modifier.height(16.dp))

                LabeledInputField(
                    label = "Shop ID",
                    value = state.shopId,
                    maxLength = 20,
                    onValueChange = viewModel::updateShopId,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditEnabled
                )

                Spacer(modifier = Modifier.height(16.dp))

                DropdownField(
                    label = "Shop Type",
                    selectedValue = state.shopType?.name ?: "Select Type",
                    options = listOf(
                        ShopType.Grocery.name,
                        ShopType.Cafeteria.name,
                        ShopType.Hyper.name,
                        ShopType.Super.name
                    ),
                    onOptionSelected = { selected ->
                        viewModel.updateShopType(ShopType.valueOf(selected))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditEnabled
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Shop Opening Date with Hijri Calendar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Shop Opening Date",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = state.shopOpeningDate?.let {
                            gregorianDateFormat.format(Date(it))
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
                                        state.shopOpeningDate?.let {
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
                                                viewModel.updateShopOpeningDate(selectedCalendar.timeInMillis)
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
                        placeholder = { Text("Select opening date") },
                        supportingText = {
                            state.shopOpeningDate?.let {
                                Text(
                                    text = "Hijri: ${HijriDateUtils.getHijriDateDayMonth(it)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // License Expiry Date Picker
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "License Expiry Date",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = state.licenseExpiryDate?.let {
                            gregorianDateFormat.format(Date(it))
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
                                        state.licenseExpiryDate?.let {
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
                                                viewModel.updateLicenseExpiryDate(selectedCalendar.timeInMillis)
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
                        placeholder = { Text("Select expiry date") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.LightGray)
                Spacer(modifier = Modifier.height(24.dp))

                // ============ ZAKATH INFORMATION SECTION ============
                SectionHeader(text = "Zakath Information")

                Spacer(modifier = Modifier.height(16.dp))

                // Stock Value Input
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Current Stock Value",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = state.stockValue,
                        onValueChange = viewModel::updateStockValue,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("Enter stock value") },
                        prefix = { Text("AED ") },
                        enabled = isEditEnabled
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stock Taken Date
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Stock Taken Date",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = state.stockTakenDate?.let {
                            gregorianDateFormat.format(Date(it))
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
                                        state.stockTakenDate?.let {
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
                                                viewModel.updateStockTakenDate(selectedCalendar.timeInMillis)
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
                        placeholder = { Text("Select date") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Zakath Amount Display (Auto-calculated)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Zakath Amount (2.5%)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "AED ${String.format("%.2f", zakathAmount)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DropdownField(
                    label = "Zakath Status",
                    selectedValue = state.zakathStatus?.name ?: "Select Status",
                    options = listOf(
                        ZakathStatus.Paid.name,
                        ZakathStatus.Pending.name,
                        ZakathStatus.NotApplicable.name
                    ),
                    onOptionSelected = { selected ->
                        viewModel.updateZakathStatus(ZakathStatus.valueOf(selected))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditEnabled
                )

                // ============ INVESTORS SECTION (only for existing shops) ============
                if (shopId != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.LightGray)
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(text = "Investors")
                        OutlinedButton(
                            onClick = { onAddInvestorClick(shopId) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Add", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (shopInvestors.isEmpty()) {
                        Text(
                            text = "No investors yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        shopInvestors.forEach { investor ->
                            ShopInvestorRow(investor = investor)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
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
                    val buttonText = if (isLoaded) "Save Changes" else "Create Shop"
                    Button(
                        onClick = {
                            viewModel.saveShop(
                                context = context,
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "Shop saved successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onFail = { errorMessage ->
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT)
                                        .show()
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

@Composable
private fun ShopInvestorRow(investor: ShopInvestorSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = investor.investorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Paid: AED ${String.format("%,.2f", investor.totalPaid)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "${String.format("%.1f", investor.sharePercentage)}%",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}