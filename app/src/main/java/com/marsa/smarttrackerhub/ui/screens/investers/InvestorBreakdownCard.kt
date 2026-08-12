package com.marsa.smarttrackerhub.ui.screens.investers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marsa.smarttracker.ui.theme.semanticStatusColors
import com.marsa.smarttrackerhub.domain.ShopInvestorSummary
import com.marsa.smarttrackerhub.ui.components.DetailSectionCard
import com.marsa.smarttrackerhub.ui.components.MetricCell

/**
 * "Investor Breakdown" — one [DetailSectionCard] holding every investor as a divider-separated
 * row (share %, status, paid / fair share / balance), matching the Statement card's chrome
 * instead of a stack of individually-elevated per-investor cards.
 */
@Composable
fun InvestorBreakdownSection(
    investors: List<ShopInvestorSummary>,
    totalShopCapital: Double,
    isAdmin: Boolean,
    onEditShareClick: (ShopInvestorSummary) -> Unit,
    onWithdrawClick: (ShopInvestorSummary) -> Unit
) {
    DetailSectionCard(title = "Investor Breakdown") {
        if (investors.isEmpty()) {
            Text(
                text = "No investors assigned yet. Tap the person icon to assign one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                investors.forEachIndexed { index, investor ->
                    InvestorRow(
                        investor = investor,
                        totalShopCapital = totalShopCapital,
                        isAdmin = isAdmin,
                        onEditShareClick = { onEditShareClick(investor) },
                        onWithdrawClick = { onWithdrawClick(investor) }
                    )
                    if (index < investors.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun InvestorRow(
    investor: ShopInvestorSummary,
    totalShopCapital: Double,
    isAdmin: Boolean,
    onEditShareClick: () -> Unit,
    onWithdrawClick: () -> Unit
) {
    val status = semanticStatusColors()
    val fairShare = if (totalShopCapital > 0) (investor.sharePercentage / 100.0) * totalShopCapital else 0.0
    val balance = investor.totalPaid - fairShare
    val isActive = investor.status == "Active"
    val balanceColor = if (balance >= 0) status.success else status.danger

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = investor.investorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Share: ${"%.0f".format(investor.sharePercentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = investor.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                if (isAdmin) {
                    IconButton(onClick = onEditShareClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit share %",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isActive) {
                        IconButton(onClick = onWithdrawClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.ExitToApp,
                                contentDescription = "Withdraw investor",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricCell("Paid", investor.totalPaid, valueColor = MaterialTheme.colorScheme.onSurface)
                MetricCell("Fair Share", fairShare, valueColor = MaterialTheme.colorScheme.onSurface)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricCell(
                    if (balance >= 0) "Overpaid" else "Underpaid",
                    kotlin.math.abs(balance),
                    valueColor = balanceColor
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
