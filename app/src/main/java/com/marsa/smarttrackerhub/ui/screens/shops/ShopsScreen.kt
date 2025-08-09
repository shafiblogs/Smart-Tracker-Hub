package com.marsa.smarttrackerhub.ui.screens.shops

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.marsa.smarttracker.ui.theme.sTypography


/**
 * Created by Muhammed Shafi on 31/05/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun ShopsScreen() {
    val firebaseApp = FirebaseApp.getInstance("SmartTrackerApp")
    val viewModel: ShopsViewModel = viewModel(factory = ShopsViewModelFactory(firebaseApp))
    val context = LocalContext.current

    val shops by viewModel.shops.collectAsState()
    val pdfUrls by viewModel.pdfUrls.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(shops) { shop ->
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = shop.name ?: "", style = sTypography.bodyMedium)
                Text(text = shop.address ?: "", style = sTypography.bodyMedium)
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "PDF Files",
                style = sTypography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        items(pdfUrls) { url ->
            Text(
                text = url,
                style = sTypography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clickable {
                        openPdf(context = context, url)
                    }
            )
        }
    }
}

