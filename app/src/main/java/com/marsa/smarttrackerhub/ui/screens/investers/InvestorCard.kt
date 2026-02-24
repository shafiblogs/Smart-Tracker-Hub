package com.marsa.smarttrackerhub.ui.screens.investers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marsa.smarttrackerhub.data.entity.InvestorInfo

/**
 * Created by Muhammed Shafi on 17/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun InvestorCard(
    investor: InvestorInfo,
    totalInvested: Double,
    shopCount: Int,
    onCardClick: () -> Unit
) {
    // Card handles the ripple natively — no extra clickable wrapper needed.
    // Shape uses the theme token (shapes.large = 16.dp by default in M3).
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onCardClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ── Investor name ──────────────────────────────────────────────
            Text(
                text = investor.investorName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (investor.investorEmail.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = investor.investorEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Phone + Shops row ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoColumn(
                    label = "Phone",
                    value = investor.investorPhone.ifBlank { "—" }
                )
                InfoColumn(
                    label = "Shops",
                    value = shopCount.toString(),
                    alignment = Alignment.End
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // ── Total invested ─────────────────────────────────────────────
            InfoColumn(
                label = "Total Invested",
                value = "AED ${String.format("%,.2f", totalInvested)}",
                valueColor = MaterialTheme.colorScheme.primary,
                valueFontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InfoColumn(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    valueFontWeight: FontWeight = FontWeight.Medium,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(horizontalAlignment = alignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = valueFontWeight,
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor
        )
    }
}
