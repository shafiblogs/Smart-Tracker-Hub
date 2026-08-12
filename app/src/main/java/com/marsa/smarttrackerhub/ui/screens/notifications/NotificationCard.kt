package com.marsa.smarttrackerhub.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person

import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import com.marsa.smarttracker.ui.theme.semanticStatusColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Created by Muhammed Shafi on 16/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun NotificationCard(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val status = com.marsa.smarttracker.ui.theme.semanticStatusColors()

    // Determine severity color and label
    val (severityColor, severityLabel) = when (notification.priority) {
        NotificationPriority.HIGH -> Pair(status.danger, "OVERDUE")
        NotificationPriority.MEDIUM -> Pair(status.warning, "DUE SOON")
    }

    // Calculate days remaining
    val today = System.currentTimeMillis()
    val daysRemaining = ((notification.expiryDate - today) / (1000 * 60 * 60 * 24)).toInt()
    val daysLabel = when {
        daysRemaining > 0 -> Pair(daysRemaining, "days left")
        daysRemaining < 0 -> Pair(-daysRemaining, "days over")
        else -> Pair(0, "due today")
    }

    // Determine notification category
    val category = when (notification.type) {
        NotificationType.SHOP_LICENSE_EXPIRED, NotificationType.SHOP_LICENSE_NEAR_EXPIRY -> "Shop licence"
        NotificationType.EMPLOYEE_VISA_EXPIRED, NotificationType.EMPLOYEE_VISA_NEAR_EXPIRY -> "Employee visa"
        NotificationType.ZAKATH_STOCK_DUE, NotificationType.ZAKATH_STOCK_APPROACHING -> "Zakath stock"
        NotificationType.ZAKATH_PAYMENT_PENDING -> "Zakath payment"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Severity rail
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(severityColor)
            )

            // Content
            Column(modifier = Modifier.weight(1f)) {
                // Eyebrow: status pill + category
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = severityColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = severityLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.09f.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = severityColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title: subject (e.g., employee name or shop name)
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurface
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Subtitle: detailed message + location
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            // Days remaining: right-aligned block
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Text(
                    text = daysLabel.first.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = severityColor
                )
                Text(
                    text = daysLabel.second,
                    style = MaterialTheme.typography.labelSmall,
                    color = severityColor
                )
            }
        }
    }
}