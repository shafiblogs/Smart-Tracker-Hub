package com.marsa.smarttrackerhub.ui.screens.investers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: investor name
            Text(
                text = investor.investorName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            if (investor.investorEmail.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = investor.investorEmail,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phone and shop count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoColumn(
                    label = "Phone",
                    value = investor.investorPhone
                )

                InfoColumn(
                    label = "Shops",
                    value = shopCount.toString(),
                    alignment = Alignment.End
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Total invested
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
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = valueFontWeight
            ),
            color = valueColor
        )
    }
}
