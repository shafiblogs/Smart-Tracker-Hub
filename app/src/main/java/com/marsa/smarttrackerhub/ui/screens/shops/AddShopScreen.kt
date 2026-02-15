package com.marsa.smarttrackerhub.ui.screens.shops

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttrackerhub.ui.components.DropdownField
import com.marsa.smarttrackerhub.ui.components.LabeledInputField
import com.marsa.smarttrackerhub.ui.screens.enums.ShopStatus
import com.marsa.smarttrackerhub.ui.screens.enums.ShopType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShopScreen(
    shopId: Int? = null,
    onShopCreated: () -> Unit
) {
    val viewModel: AddShopViewModel = viewModel()
    val state by viewModel.formState.collectAsState()
    val isValid by viewModel.isFormValid.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    val isLoaded by viewModel.isLoaded.collectAsState()

    LaunchedEffect(shopId) {
        shopId?.let {
            viewModel.loadShop(context, it)
        }
    }

    LaunchedEffect(isSaved) {
        if (isSaved) {
            onShopCreated()
        }
    }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
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

                LabeledInputField(
                    label = "Shop Name",
                    value = state.shopName,
                    maxLength = 20,
                    onValueChange = viewModel::updateShopName,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                LabeledInputField(
                    label = "Address",
                    value = state.shopAddress,
                    maxLength = 30,
                    onValueChange = viewModel::updateShopAddress,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                LabeledInputField(
                    label = "Shop ID",
                    value = state.shopId,
                    maxLength = 20,
                    onValueChange = viewModel::updateShopId,
                    modifier = Modifier.fillMaxWidth()
                )

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
                            dateFormat.format(Date(it))
                        } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
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
                                            selectedCalendar.set(year, month, dayOfMonth, 0, 0, 0)
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
                        },
                        placeholder = { Text("Select expiry date") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                DropdownField(
                    label = "Shop Status",
                    selectedValue = state.shopStatus?.name ?: "Select Status",
                    options = listOf(
                        ShopStatus.Running.name,
                        ShopStatus.Initial.name,
                        ShopStatus.Closed.name
                    ),
                    onOptionSelected = { selected ->
                        viewModel.updateShopStatus(ShopStatus.valueOf(selected))
                    },
                    modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.fillMaxWidth()
                )

                if (!error.isNullOrEmpty()) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                val buttonText = if (isLoaded) "Update Shop" else "Create Shop"
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
                                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    enabled = isValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = buttonText, fontSize = 18.sp)
                }
            }
        }
    }
}