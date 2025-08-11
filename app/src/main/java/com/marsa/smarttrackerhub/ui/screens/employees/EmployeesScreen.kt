package com.marsa.smarttrackerhub.ui.screens.employees

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsa.smarttracker.ui.theme.sTypography
import com.marsa.smarttrackerhub.ui.components.CommonTextField


/**
 * Created by Muhammed Shafi on 11/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

@Composable
fun EmployeesScreen(onAddClick: () -> Unit, onItemClick: (Int) -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        CommonTextField(
            value = "Summary Screen",
            style = sTypography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp
            )
        )

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
}