package com.marsa.smarttrackerhub.ui.screens.shops

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttracker.ui.theme.sTypography


/**
 * Created by Muhammed Shafi on 31/05/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun ShopsScreen() {
    val viewModel: ShopsViewModel = viewModel()
    val shops by viewModel.shops.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(shops) { shop ->
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = shop.name ?: "",
                    style = sTypography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = shop.address ?: "",
                    style = sTypography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}