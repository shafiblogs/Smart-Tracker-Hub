package com.marsa.smarttrackerhub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marsa.smarttracker.ui.theme.spaceLg
import com.marsa.smarttracker.ui.theme.spaceMd
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Standard "pick any month" sheet: a year stepper over a 3×4 grid of month chips. Replaces the
 * day-granularity Material3 `DatePicker` — it has no month-only mode, and truncating a day grid
 * to a month would show the user a control whose day is ignored, which reads as broken.
 *
 * [availableMonthKeys] dims (but does not disable) months this screen has no data for, when
 * supplied — pass null to leave every month full-strength (e.g. nothing to check against yet,
 * as with Home today). Future months and anything before [earliestMonth] are disabled outright.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthGridPicker(
    initialMonth: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
    availableMonthKeys: Set<YearMonth>? = null,
    earliestMonth: YearMonth? = null
) {
    var displayedYear by remember { mutableIntStateOf(initialMonth.year) }
    val now = remember { YearMonth.now() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spaceLg, vertical = spaceMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { displayedYear-- },
                    enabled = earliestMonth == null || displayedYear > earliestMonth.year
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous year")
                }
                Text(
                    text = displayedYear.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                IconButton(
                    onClick = { displayedYear++ },
                    enabled = displayedYear < now.year
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next year")
                }
            }

            Spacer(modifier = Modifier.height(spaceMd))

            val months = remember(displayedYear) { (1..12).map { YearMonth.of(displayedYear, it) } }
            Column(verticalArrangement = Arrangement.spacedBy(spaceMd)) {
                months.chunked(3).forEach { rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spaceMd)
                    ) {
                        rowMonths.forEach { month ->
                            val isFuture = month.isAfter(now)
                            val isBeforeFloor = earliestMonth != null && month.isBefore(earliestMonth)
                            val hasData = availableMonthKeys == null || month in availableMonthKeys
                            MonthChip(
                                label = month.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                                selected = month == initialMonth,
                                enabled = !isFuture && !isBeforeFloor,
                                dimmed = !hasData,
                                onClick = { onMonthSelected(month) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spaceMd))
        }
    }
}

@Composable
private fun MonthChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val background = when {
        selected -> colors.primary
        else -> colors.surfaceVariant.copy(alpha = if (enabled) 0.5f else 0.25f)
    }
    val contentColor = when {
        !enabled -> colors.onSurfaceVariant.copy(alpha = 0.35f)
        selected -> colors.onPrimary
        dimmed -> colors.onSurfaceVariant.copy(alpha = 0.55f)
        else -> colors.onSurface
    }
    Box(
        modifier = modifier
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = contentColor,
            textAlign = TextAlign.Center
        )
    }
}
