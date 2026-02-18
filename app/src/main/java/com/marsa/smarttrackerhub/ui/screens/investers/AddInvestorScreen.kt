package com.marsa.smarttrackerhub.ui.screens.investers

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.marsa.smarttrackerhub.ui.components.LabeledInputField


/**
 * Created by Muhammed Shafi on 11/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

@Composable
fun AddInvestorScreen(
    investorId: Int? = null,
    onSaveSuccess: () -> Unit
) {
    val viewModel: InvestorAddViewModel = viewModel()
    val state by viewModel.formState.collectAsState()
    val isValid by viewModel.isFormValid.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val isLoaded by viewModel.isLoaded.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    // View mode (read-only) when editing an existing investor
    var isEditEnabled by remember { mutableStateOf(investorId == null) }

    LaunchedEffect(investorId) {
        investorId?.let {
            viewModel.loadInvestor(context, it)
            isEditEnabled = false
        }
    }

    LaunchedEffect(isSaved) {
        if (isSaved) onSaveSuccess()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            // Show edit FAB only when viewing an existing investor in read-only mode
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
                SectionHeader(text = "Investor Information")

                Spacer(modifier = Modifier.height(16.dp))

                LabeledInputField(
                    label = "Full Name *",
                    value = state.investorName,
                    maxLength = 50,
                    onValueChange = viewModel::updateName,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditEnabled
                )
                if (state.nameError != null) {
                    Text(
                        text = state.nameError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Email — optional, OutlinedTextField for email keyboard type
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Email",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isEditEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = state.investorEmail,
                        onValueChange = viewModel::updateEmail,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEditEnabled,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        placeholder = { Text("Enter email address") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LabeledInputField(
                    label = "Phone *",
                    value = state.investorPhone,
                    maxLength = 20,
                    onValueChange = viewModel::updatePhone,
                    keyboardType = KeyboardType.Phone,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditEnabled
                )
                if (state.phoneError != null) {
                    Text(
                        text = state.phoneError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
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

                if (isEditEnabled) {
                    val buttonText = if (isLoaded) "Save Changes" else "Add Investor"
                    Button(
                        onClick = {
                            viewModel.saveInvestor(
                                context = context,
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "Investor saved successfully!",
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