package com.marsa.smarttrackerhub.ui.screens.shops

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import com.marsa.smarttrackerhub.ui.screens.enums.ShopStatus
import com.marsa.smarttrackerhub.utils.HijriDateUtils
import com.marsa.smarttrackerhub.utils.getExpiryStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Created by Muhammed Shafi on 15/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun ShopCard(
    shop: ShopInfo,
    totalInvested: Double = 0.0,
    onCardClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    val expiryStatus = shop.licenseExpiryDate.getExpiryStatus()
    val zakathAmount = shop.stockValue * 0.025 // 2.5% for Zakath
    val shopStatus =
        runCatching { ShopStatus.valueOf(shop.shopStatus) }.getOrDefault(ShopStatus.Initial)
    val shopStatusColor = when (shopStatus) {
        ShopStatus.Running -> MaterialTheme.colorScheme.primary
        ShopStatus.Closed -> MaterialTheme.colorScheme.error
        ShopStatus.Initial -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shopStatusIcon = when (shopStatus) {
        ShopStatus.Running -> Icons.Default.PlayArrow
        ShopStatus.Closed -> Icons.Default.Close
        ShopStatus.Initial -> Icons.Outlined.Warning
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Shop Name + Share Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shop.shopName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = shop.shopAddress,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }

                // Share button
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Shop",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Shop Status Badge — full-width pill
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = shopStatusColor.copy(alpha = 0.10f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = shopStatusIcon,
                        contentDescription = null,
                        tint = shopStatusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Shop Status: ${shopStatus.name}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = shopStatusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shop ID and Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoColumn(
                    label = "Shop ID",
                    value = shop.shopId
                )

                InfoColumn(
                    label = "Type",
                    value = shop.shopType,
                    alignment = Alignment.End
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Opening Date with English and Arabic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoColumn(
                    label = "Opening Date",
                    value = dateFormat.format(Date(shop.shopOpeningDate))
                )

                InfoColumn(
                    label = "تاريخ الافتتاح",
                    value = HijriDateUtils.getHijriDateDayMonth(shop.shopOpeningDate),
                    valueColor = MaterialTheme.colorScheme.primary,
                    alignment = Alignment.End
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // Zakath Status and Amount
            val zakathStatusColor = when (shop.zakathStatus) {
                "Paid" -> MaterialTheme.colorScheme.primary
                "Pending" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoColumn(
                    label = "Zakath Status",
                    value = shop.zakathStatus,
                    valueColor = zakathStatusColor
                )

                InfoColumn(
                    label = "Zakath Amount",
                    value = "AED ${String.format("%.2f", zakathAmount)}",
                    valueColor = MaterialTheme.colorScheme.primary,
                    valueFontWeight = FontWeight.Bold,
                    alignment = Alignment.End
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stock Value and Stock Taken Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoColumn(
                    label = "Stock Value",
                    value = "AED ${String.format("%.2f", shop.stockValue)}"
                )

                InfoColumn(
                    label = "Stock Taken",
                    value = dateFormat.format(Date(shop.stockTakenDate)),
                    alignment = Alignment.End
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Total Invested
            InfoColumn(
                label = "Total Invested",
                value = "AED ${String.format("%,.2f", totalInvested)}",
                valueColor = MaterialTheme.colorScheme.primary,
                valueFontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // License Expiry with Status Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoColumn(
                    label = "License Expiry",
                    value = dateFormat.format(Date(shop.licenseExpiryDate))
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = CircleShape,
                        color = expiryStatus.color,
                        modifier = Modifier.size(10.dp)
                    ) {}

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = expiryStatus.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = expiryStatus.color
                    )
                }
            }
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
    Column(
        horizontalAlignment = alignment
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = valueFontWeight
            ),
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor
        )
    }
}
