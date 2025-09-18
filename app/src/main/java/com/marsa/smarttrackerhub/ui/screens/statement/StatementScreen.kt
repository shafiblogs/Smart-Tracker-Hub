package com.marsa.smarttrackerhub.ui.screens.statement

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp


/**
 * Created by Muhammed Shafi on 31/05/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementScreen(isGuestUser: Boolean) {
    val firebaseApp = FirebaseApp.getInstance("SmartTrackerApp")
    val viewModel: StatementViewModel = viewModel(factory = StatementViewModelFactory(firebaseApp))
    val context = LocalContext.current

    val shops by viewModel.shops.collectAsState()
    var selectedShop by remember { mutableStateOf<StatementDto?>(null) }
    var expanded by remember { mutableStateOf(false) }

    // Load data once when screen is first shown
    LaunchedEffect(isGuestUser) {
        if (!isGuestUser) viewModel.loadScreenData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Dropdown for shop selection
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = selectedShop?.name ?: "Select Shop",
                onValueChange = {},
                readOnly = true,
                label = { Text("Shop Name") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                shops.forEach { shop ->
                    DropdownMenuItem(
                        text = { Text(shop.name ?: "-") },
                        onClick = {
                            selectedShop = shop
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Show message or selected shop card
        if (selectedShop == null) {
            Text(
                text = "Please select a shop to view its statement",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            StatementCard(
                statement = selectedShop!!,
                onViewPdf = { url -> openPdf(context, url) }
            )
        }
    }
}

@Composable
fun StatementCard(statement: StatementDto, onViewPdf: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = statement.name ?: "-",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (statement.statementFiles.isEmpty()) {
                Text(
                    text = "No statements available",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                statement.statementFiles.forEach { file ->
                    Button(
                        onClick = { onViewPdf(file.url) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(file.month)
                    }
                }
            }
        }
    }
}
