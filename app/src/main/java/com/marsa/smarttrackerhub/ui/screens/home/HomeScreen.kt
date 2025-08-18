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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    entry: MonthlySummary,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onDelete() }) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Shop Name - bigger and bold
            val shopName = when (entry.shopId) {
                "MARSA_101" -> "Al Marsa Grocery - Muzeira"
                "MARSA_102" -> "Al Marsa Grocery - Masfooth"
                "WADI_101" -> "Al Wadi Cafe - Muzeira"
                else -> "Al Wadi Cafe - Muzeira"
            }

            Text(
                text = shopName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(12.dp))

            // Balances Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Balances",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Opening",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Current",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            Divider(Modifier.padding(vertical = 6.dp))

            BalanceComparisonRow("Cash", entry.openingCashBalance, entry.cashBalance)
            BalanceComparisonRow("Account", entry.openingAccountBalance, entry.accountBalance)
            BalanceComparisonRow("Credit", entry.openingCreditBalance, entry.creditSaleBalance)

            Divider(Modifier.padding(vertical = 8.dp))

            // Totals Section
            InfoRow("💰 Total Sale", entry.totalSales)
            InfoRow("🛒 Total Purchase", entry.totalPurchases)
            InfoRow("💳 Total Expense", entry.totalExpenses)
            InfoRow("💰 Credit Sale Payment", entry.creditSalePayment, isHighlight = true)
        }
    }
}

@Composable
fun BalanceComparisonRow(label: String, opening: Double, current: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "₹%.2f".format(opening),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "₹%.2f".format(current),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = if (current < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun InfoRow(label: String, value: Double, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "₹%.2f".format(value),
            style = if (isHighlight) {
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                MaterialTheme.typography.bodyMedium
            }
        )
    }
}





