package com.marsa.smarttrackerhub.ui.screens.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttracker.ui.theme.sTypography
import com.marsa.smarttrackerhub.utils.ShareUtil
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(viewModel: LogsViewModel = viewModel()) {
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.initDatabase(context) }

    val selectionItems   by viewModel.selectionItems.collectAsState()
    val selectedItem     by viewModel.selectedItem.collectAsState()
    val selectedMonth    by viewModel.selectedMonth.collectAsState()
    val isLoading        by viewModel.isLoading.collectAsState()

    // Shop mode
    val shopDaySummaries by viewModel.shopDaySummaries.collectAsState()
    val shopMonthSummary by viewModel.shopMonthSummary.collectAsState()

    // Employee mode
    val employeeDayRecords   by viewModel.employeeDayRecords.collectAsState()
    val employeeMonthSummary by viewModel.employeeMonthSummary.collectAsState()

    var selectionExpanded by remember { mutableStateOf(false) }
    var monthExpanded     by remember { mutableStateOf(false) }

    // View refs for sharing
    val summaryViewRef  = remember { mutableStateOf<android.view.View?>(null) }
    val dayCardViewRefs = remember { mutableMapOf<String, android.view.View>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // ── Selection dropdown (Shops + Employees) ─────────────────────────
        ExposedDropdownMenuBox(
            expanded         = selectionExpanded,
            onExpandedChange = { selectionExpanded = it }
        ) {
            OutlinedTextField(
                value         = selectedItem?.displayLabel() ?: "Select shop or employee",
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Shop / Employee") },
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(selectionExpanded) },
                modifier      = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded         = selectionExpanded,
                onDismissRequest = { selectionExpanded = false }
            ) {
                // ── Shops section ──
                val shops     = selectionItems.filterIsInstance<SelectionItem.ShopItem>()
                val employees = selectionItems.filterIsInstance<SelectionItem.EmployeeItem>()

                if (shops.isNotEmpty()) {
                    DropdownMenuItem(
                        text    = {
                            Text(
                                text  = "— Shops —",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick  = {},
                        enabled  = false
                    )
                    shops.forEach { item ->
                        DropdownMenuItem(
                            text    = { Text(item.shop.shopName) },
                            onClick = {
                                viewModel.selectItem(item)
                                selectionExpanded = false
                            }
                        )
                    }
                }

                if (employees.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text    = {
                            Text(
                                text  = "— Employees —",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick  = {},
                        enabled  = false
                    )
                    employees.forEach { item ->
                        DropdownMenuItem(
                            text    = {
                                Column {
                                    Text(item.employee.employeeName, style = MaterialTheme.typography.bodyMedium)
                                    Text(item.shopName, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                viewModel.selectItem(item)
                                selectionExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Month dropdown ─────────────────────────────────────────────────
        ExposedDropdownMenuBox(
            expanded         = monthExpanded,
            onExpandedChange = { monthExpanded = it }
        ) {
            OutlinedTextField(
                value         = selectedMonth.displayName,
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Month") },
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(monthExpanded) },
                modifier      = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded         = monthExpanded,
                onDismissRequest = { monthExpanded = false }
            ) {
                viewModel.availableMonths.forEach { month ->
                    DropdownMenuItem(
                        text    = { Text(month.displayName) },
                        onClick = {
                            viewModel.selectMonth(month)
                            monthExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Content ────────────────────────────────────────────────────────
        when {
            selectedItem == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text  = "Select a shop or employee to view logs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            selectedItem is SelectionItem.ShopItem -> {
                // ── Shop view ──────────────────────────────────────────────
                val summary = shopMonthSummary
                if (shopDaySummaries.isEmpty() && summary == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text  = "No logs for ${selectedMonth.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Summary card (shareable)
                        if (summary != null) {
                            item(key = "shop_summary") {
                                AndroidView(
                                    factory = { ctx ->
                                        ComposeView(ctx).apply {
                                            setContent {
                                                ShopSummaryCard(
                                                    summary = summary,
                                                    onRefresh = { viewModel.refresh() },
                                                    onShare = {
                                                        summaryViewRef.value?.let {
                                                            ShareUtil.shareViewAsImage(it, ctx,
                                                                "shop_summary_${summary.shopName.replace(" ", "_")}_${selectedMonth.key}.png",
                                                                "Share Shop Summary")
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    update = { view ->
                                        summaryViewRef.value = view
                                        (view as? ComposeView)?.setContent {
                                            ShopSummaryCard(
                                                summary = summary,
                                                onRefresh = { viewModel.refresh() },
                                                onShare = {
                                                    ShareUtil.shareViewAsImage(view, context,
                                                        "shop_summary_${summary.shopName.replace(" ", "_")}_${selectedMonth.key}.png",
                                                        "Share Shop Summary")
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Day cards
                        items(shopDaySummaries, key = { it.date }) { day ->
                            AndroidView(
                                factory = { ctx ->
                                    ComposeView(ctx).apply {
                                        setContent {
                                            ShopDayCard(day = day, onShare = {
                                                dayCardViewRefs[day.date]?.let {
                                                    ShareUtil.shareViewAsImage(it, ctx,
                                                        "shop_log_${day.date}.png", "Share Log")
                                                }
                                            })
                                        }
                                    }
                                },
                                update = { view ->
                                    dayCardViewRefs[day.date] = view
                                    (view as? ComposeView)?.setContent {
                                        ShopDayCard(day = day, onShare = {
                                            ShareUtil.shareViewAsImage(view, context,
                                                "shop_log_${day.date}.png", "Share Log")
                                        })
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }

            selectedItem is SelectionItem.EmployeeItem -> {
                // ── Employee view ──────────────────────────────────────────
                val summary = employeeMonthSummary
                if (employeeDayRecords.isEmpty() && summary == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text  = "No logs for ${selectedMonth.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Summary card (shareable)
                        if (summary != null) {
                            item(key = "emp_summary") {
                                AndroidView(
                                    factory = { ctx ->
                                        ComposeView(ctx).apply {
                                            setContent {
                                                EmployeeSummaryCard(
                                                    summary = summary,
                                                    onRefresh = { viewModel.refresh() },
                                                    onShare = {
                                                        summaryViewRef.value?.let {
                                                            ShareUtil.shareViewAsImage(it, ctx,
                                                                "emp_summary_${summary.employeeName.replace(" ", "_")}_${selectedMonth.key}.png",
                                                                "Share Employee Summary")
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    update = { view ->
                                        summaryViewRef.value = view
                                        (view as? ComposeView)?.setContent {
                                            EmployeeSummaryCard(
                                                summary = summary,
                                                onRefresh = { viewModel.refresh() },
                                                onShare = {
                                                    ShareUtil.shareViewAsImage(view, context,
                                                        "emp_summary_${summary.employeeName.replace(" ", "_")}_${selectedMonth.key}.png",
                                                        "Share Employee Summary")
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Day cards
                        items(employeeDayRecords, key = { it.date }) { day ->
                            AndroidView(
                                factory = { ctx ->
                                    ComposeView(ctx).apply {
                                        setContent {
                                            EmployeeDayCard(day = day, onShare = {
                                                dayCardViewRefs[day.date]?.let {
                                                    ShareUtil.shareViewAsImage(it, ctx,
                                                        "emp_log_${day.date}.png", "Share Log")
                                                }
                                            })
                                        }
                                    }
                                },
                                update = { view ->
                                    dayCardViewRefs[day.date] = view
                                    (view as? ComposeView)?.setContent {
                                        EmployeeDayCard(day = day, onShare = {
                                            ShareUtil.shareViewAsImage(view, context,
                                                "emp_log_${day.date}.png", "Share Log")
                                        })
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ── Shop Summary Card ─────────────────────────────────────────────────────────

@Composable
private fun ShopSummaryCard(
    summary: ShopMonthSummary,
    onRefresh: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = summary.shopName,
                        style = sTypography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text  = summary.monthDisplay,
                        style = sTypography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp))
                }
                TextButton(onClick = onRefresh,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh",
                        modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(
                modifier  = Modifier.padding(vertical = 8.dp),
                color     = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStatColumn("Days Open",  "${summary.totalDaysOpen}")
                SummaryStatColumn("Total",      summary.totalMinutes.toHoursLabel())
                SummaryStatColumn("Avg / Day",  summary.avgMinutesPerDay.toHoursLabel())
            }
        }
    }
}

// ── Employee Summary Card ─────────────────────────────────────────────────────

@Composable
private fun EmployeeSummaryCard(
    summary: EmployeeMonthSummary,
    onRefresh: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = summary.employeeName,
                        style = sTypography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text  = "${summary.shopName} · ${summary.monthDisplay}",
                        style = sTypography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp))
                }
                TextButton(onClick = onRefresh,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh",
                        modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(
                modifier  = Modifier.padding(vertical = 8.dp),
                color     = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStatColumn("Days Worked", "${summary.totalDays}")
                SummaryStatColumn("Total",       summary.totalMinutes.toHoursLabel())
                SummaryStatColumn("Avg / Day",   summary.avgMinutesPerDay.toHoursLabel())
            }
        }
    }
}

@Composable
private fun SummaryStatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = value,
            style = sTypography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text      = label,
            style     = sTypography.bodySmall.copy(fontSize = 10.sp),
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ── Shop Day Card ─────────────────────────────────────────────────────────────

@Composable
private fun ShopDayCard(day: DayLogSummary, onShare: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = formatLogDate(day.date),
                    style    = sTypography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color    = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (day.shopOpenTime == null) {
                Text("Not opened", style = sTypography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            } else {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TimeChip(label = "Open",  time = day.shopOpenTime,  color = Color(0xFF22C55E))
                    Text("→", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (day.shopCloseTime != null) {
                        TimeChip(label = "Close", time = day.shopCloseTime, color = Color(0xFFEF4444))
                    } else {
                        Text("Still open", style = sTypography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF22C55E))
                    }
                    day.shopTotalMinutes?.let { mins ->
                        Spacer(modifier = Modifier.weight(1f))
                        Text(mins.toHoursLabel(),
                            style = sTypography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// ── Employee Day Card ─────────────────────────────────────────────────────────

@Composable
private fun EmployeeDayCard(day: EmployeeDayRecord, onShare: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            // Date header
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = formatLogDate(day.date),
                    style    = sTypography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color    = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (day.sessions.isEmpty()) {
                Text("No record", style = sTypography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            } else {
                // Sessions
                day.sessions.forEachIndexed { idx, session ->
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (day.sessions.size > 1) {
                            Text("S${idx + 1} ", style = sTypography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.width(20.dp))
                        } else {
                            Spacer(modifier = Modifier.width(20.dp))
                        }
                        Text(formatLogTime(session.loginTime),
                            style = sTypography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFF22C55E))
                        Text(" → ", style = sTypography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text  = if (session.logoutTime != null) formatLogTime(session.logoutTime)
                                    else "Working",
                            style = sTypography.bodySmall.copy(fontSize = 12.sp),
                            color = if (session.logoutTime == null) Color(0xFFF97316)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (session.durationMinutes > 0) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(session.durationMinutes.toHoursLabel(),
                                style = sTypography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                // Day total (if multiple sessions or single with duration)
                if (day.totalMinutes > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Total: ${day.totalMinutes.toHoursLabel()}",
                            style = sTypography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// ── Shared chip ───────────────────────────────────────────────────────────────

@Composable
private fun TimeChip(label: String, time: Long, color: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, style = sTypography.bodySmall.copy(fontSize = 9.sp), color = color.copy(alpha = 0.7f))
        Text(formatLogTime(time),
            style = sTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
            color = color)
    }
}

// ── Utilities ─────────────────────────────────────────────────────────────────

internal fun formatLogDate(dateStr: String): String {
    return try {
        LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy, EEEE"))
    } catch (e: Exception) { dateStr }
}

private fun formatLogTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    return try { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp)) }
    catch (e: Exception) { "" }
}
