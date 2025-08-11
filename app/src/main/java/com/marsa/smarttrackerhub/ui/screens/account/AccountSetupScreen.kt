package com.marsa.smarttrackerhub.ui.screens.account

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttrackerhub.ui.components.LabeledInputField

/**
 * Created by Muhammed Shafi on 18/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun AccountSetupScreen(
    onAccountCreated: () -> Unit
) {
    val viewModel: AccountSetupViewModel = viewModel()
    val state by viewModel.formState.collectAsState()
    val isValid by viewModel.isFormValid.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    val isLoaded by viewModel.isLoaded.collectAsState()

    LaunchedEffect(isSaved) {
        viewModel.loadExistingAccount(context)
        if (isSaved) {
            onAccountCreated()
        }
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

                LabeledInputField(
                    label = "Account Name",
                    value = state.accountName,
                    maxLength = 20,
                    onValueChange = viewModel::updateAccountName,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                LabeledInputField(
                    label = "Address",
                    value = state.address,
                    maxLength = 30,
                    onValueChange = viewModel::updateAddress,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                LabeledInputField(
                    label = "User Code",
                    value = state.userCode,
                    maxLength = 20,
                    onValueChange = viewModel::updateUserCode,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                LabeledInputField(
                    label = "User Name",
                    value = state.userName,
                    maxLength = 20,
                    onValueChange = viewModel::updateUserName,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                LabeledInputField(
                    label = "Password",
                    value = state.password,
                    onValueChange = viewModel::updatePassword,
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                LabeledInputField(
                    label = "Confirm Password",
                    value = state.confirmPassword,
                    onValueChange = viewModel::updateConfirmPassword,
                    keyboardType = KeyboardType.Text,
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
                val buttonText = if (isLoaded) "Update Account" else "Create Account"
                Button(
                    onClick = {
                        viewModel.saveAccount(
                            context = context,
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    "Account saved successfully!",
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = buttonText, fontSize = 18.sp)
                }
            }
        }
    }
}

