package com.marsa.smarttrackerhub.ui.screens.investments

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marsa.smarttracker.ui.theme.semanticStatusColors
import com.marsa.smarttrackerhub.ui.screens.enums.ShopStatus
import com.marsa.smarttrackerhub.utils.formatMoney

/**
 * One shop on the Investments list.
 *
 *   name ······························ [status pill]
 *   Đ1,250,000 ····················· 44% of portfolio
 *   ▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░
 *   4 investors ································· ›
 *
 * Capital carries the visual weight, and the bar under it is that shop's share of the portfolio —
 * so shops can be ranked by scanning bar lengths rather than diffing money strings. Allocation %
 * is *not* drawn: shares sum to 100 for every shop, so a bar would be full on every card. It
 * appears as a coloured warning beside the chevron only when it deviates from 100. Closed shops
 * dim but stay readable and tappable. Tapping opens the full ShopInvestmentDashboard.
 */
@Composable
fun InvestmentShopCard(
    row: InvestmentShopRow,
    totalPortfolioCapital: Double,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val statusColors = semanticStatusColors()
    val status = runCatching { ShopStatus.valueOf(row.shopStatus) }.getOrDefault(ShopStatus.Initial)
    val statusColor = when (status) {
        ShopStatus.Running -> statusColors.success
        ShopStatus.Closed -> colors.error
        ShopStatus.Initial -> statusColors.warning
    }
    val isClosed = status == ShopStatus.Closed
    val portfolioShare =
        if (totalPortfolioCapital > 0) (row.totalCapital / totalPortfolioCapital) * 100 else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .let { if (isClosed) it.alpha(0.72f) else it },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {

            // Name + status pill, one row.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.shopName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = status.name,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Capital headline + share of portfolio.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = formatMoney(row.totalCapital, 0),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurface
                )
                Text(
                    text = "${"%.0f".format(portfolioShare)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            PortfolioShareBar(sharePercentage = portfolioShare)
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "share of portfolio capital",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Investor count, the single chevron, and — only when allocation isn't the expected
            // 100% — a warning about unassigned or over-assigned equity.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${row.investorCount} investor${if (row.investorCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    allocationAnomalyColor(row.allocatedPercentage)?.let { anomalyColor ->
                        Text(
                            text = "${"%.0f".format(row.allocatedPercentage)}% allocated",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = anomalyColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View shop details",
                        modifier = Modifier.size(16.dp),
                        tint = colors.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
