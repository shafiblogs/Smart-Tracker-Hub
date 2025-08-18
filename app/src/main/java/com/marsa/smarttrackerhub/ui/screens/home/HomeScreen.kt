package com.marsa.smarttrackerhub.ui.screens.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.marsa.smarttrackerhub.domain.MonthlySummary
import com.marsa.smarttrackerhub.ui.components.InfoRow


/**
 * Created by Muhammed Shafi on 22/06/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

@Composable
fun HomeScreen() {
    val firebaseApp = FirebaseApp.getInstance("SmartTrackerApp")
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(firebaseApp))

    val summary by viewModel.summary.collectAsState()
    var selectedItem by remember { mutableStateOf<MonthlySummary?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        items(summary) { entry ->
            DailySummaryCard(entry, onDelete = {
                selectedItem = entry
            })
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun DailySummaryCard(
    entry: MonthlySummary, onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onDelete() })
            }
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            val shopName = when (entry.shopId) {
                "MARSA_101" -> "Al Marsa Grocery - Muzeira"
                "MARSA_102" -> "Al Marsa Grocery - Masfooth"
                "WADI_101" -> "Al Wadi Cafe - Muzeira"
                else -> "Al Wadi Cafe - Muzeira"
            }

            Text(
                text = shopName,
                fontSize = 14.sp,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Cash Balance",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹%.2f".format(entry.cashBalance),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Account Balance",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹%.2f".format(entry.accountBalance),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            InfoRow("💰 Total Sale", entry.totalSales, color = MaterialTheme.colorScheme.secondary)
            InfoRow("💳 Total Expense", entry.totalExpenses, MaterialTheme.colorScheme.primary)
            InfoRow("🛒 Total Purchase", entry.totalPurchases, MaterialTheme.colorScheme.primary)
            InfoRow(
                "💰 Credit Sale Payment",
                entry.creditSalePayment,
                color = MaterialTheme.colorScheme.secondary
            )
            InfoRow(
                "💳 Credit Sale Balance",
                entry.creditSaleBalance,
                MaterialTheme.colorScheme.primary
            )
        }
    }
}



