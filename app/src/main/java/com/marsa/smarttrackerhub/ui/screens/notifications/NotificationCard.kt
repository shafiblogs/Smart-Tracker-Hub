package com.marsa.smarttrackerhub.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsa.smarttracker.ui.theme.cardRadius
import com.marsa.smarttracker.ui.theme.semanticStatusColors
import com.marsa.smarttracker.ui.theme.spaceLg
import com.marsa.smarttracker.ui.theme.spaceSm
import com.marsa.smarttrackerhub.utils.formatDateOnly

/**
 * Created by Muhammed Shafi on 16/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 *
 * One alert row: a severity rail carries priority while scrolling, an eyebrow states status +
 * category, the subject (person/shop name) is the title, and the day count is the numeral the
 * reader is actually scanning for. Everything is derived from [NotificationItem]'s own fields —
 * no separate icon vocabulary to keep in sync with [NotificationType].
 */
@Composable
fun NotificationCard(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val status = semanticStatusColors()

    val (severityColor, severityLabel) = when (notification.priority) {
        NotificationPriority.HIGH -> status.danger to "OVERDUE"
        NotificationPriority.MEDIUM -> status.warning to "DUE SOON"
    }

    val daysRemaining = ((notification.expiryDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
    val (dayCount, dayUnit) = when {
        daysRemaining > 0 -> daysRemaining to "days left"
        daysRemaining < 0 -> -daysRemaining to "days over"
        else -> 0 to "due today"
    }

    val category = notification.type.categoryLabel()
    val subject = notification.entityName.ifBlank { notification.title }
    val subtitle = notification.dueDateSubtitle(daysRemaining)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        // height(IntrinsicSize.Min) gives the row a bounded height to measure the rail against —
        // without it, fillMaxHeight() inside an unbounded LazyColumn item collapses to 0dp.
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(severityColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(spaceLg)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spaceSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = severityColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = severityLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = severityColor,
                            modifier = Modifier.padding(horizontal = spaceSm, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(spaceSm))

                Text(
                    text = subject,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(top = spaceLg, end = spaceLg, bottom = spaceLg)
            ) {
                Text(
                    text = dayCount.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = severityColor
                )
                Text(
                    text = dayUnit,
                    style = MaterialTheme.typography.labelSmall,
                    color = severityColor
                )
            }
        }
    }
}

private fun NotificationType.categoryLabel(): String = when (this) {
    NotificationType.SHOP_LICENSE_EXPIRED, NotificationType.SHOP_LICENSE_NEAR_EXPIRY -> "Shop licence"
    NotificationType.EMPLOYEE_VISA_EXPIRED, NotificationType.EMPLOYEE_VISA_NEAR_EXPIRY -> "Employee visa"
    NotificationType.ZAKATH_STOCK_DUE, NotificationType.ZAKATH_STOCK_APPROACHING -> "Zakath stock"
    NotificationType.ZAKATH_PAYMENT_PENDING -> "Zakath payment"
}

private fun NotificationType.subjectNoun(): String = when (this) {
    NotificationType.SHOP_LICENSE_EXPIRED, NotificationType.SHOP_LICENSE_NEAR_EXPIRY -> "Licence"
    NotificationType.EMPLOYEE_VISA_EXPIRED, NotificationType.EMPLOYEE_VISA_NEAR_EXPIRY -> "Visa"
    NotificationType.ZAKATH_STOCK_DUE, NotificationType.ZAKATH_STOCK_APPROACHING -> "Stock count"
    NotificationType.ZAKATH_PAYMENT_PENDING -> "Zakath payment"
}

/**
 * "Visa expires 24 Aug 2026" / "Licence expired 3 Jan 2026" (+ additionalInfo when present).
 * Built from the item's own fields rather than [NotificationItem.message], which duplicates the
 * subject name and the relative day count the numeral already shows.
 */
private fun NotificationItem.dueDateSubtitle(daysRemaining: Int): String {
    val verb = if (daysRemaining < 0) "expired" else "expires"
    val base = "${type.subjectNoun()} $verb ${expiryDate.formatDateOnly()}"
    return additionalInfo?.takeIf { it.isNotBlank() }?.let { "$base · $it" } ?: base
}
