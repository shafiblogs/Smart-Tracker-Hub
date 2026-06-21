package com.marsa.smarttrackerhub.ui.screens.logs

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttracker.ui.theme.sTypography
import com.marsa.smarttrackerhub.utils.PdfExportUtil
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                            .padding(vertical = 4.dp)
                    )
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
                // ── Shop view (Table Format) ───────────────────────────────
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
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp)
                    ) {
                        // Single unified card with summary and records
                        item(key = "shop_unified_card") {
                            AndroidView(
                                factory = { ctx ->
                                    ComposeView(ctx).apply {
                                        setContent {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                            ) {
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    // Header row with title and action buttons
                                                    Row(
                                                        modifier              = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment     = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text  = summary?.shopName ?: "Shop",
                                                                style = MaterialTheme.typography.titleSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text  = selectedMonth.displayName,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        IconButton(onClick = {
                                                            if (summary != null) {
                                                                PdfExportUtil.generateAndShareShopLogsPdf(
                                                                    context,
                                                                    summary.shopName,
                                                                    summary.monthDisplay,
                                                                    shopDaySummaries,
                                                                    summary
                                                                )
                                                            }
                                                        }, modifier = Modifier.size(36.dp)) {
                                                            Icon(Icons.Default.Share, contentDescription = "Share",
                                                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                        }
                                                    }

                                    // Stats row
                                    if (summary != null) {
                                        Row(
                                            modifier              = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text  = "Days Open:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text  = "${summary.totalDaysOpen}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text  = "Total:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text  = summary.totalMinutes.toHoursLabel(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text  = "Avg:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text  = summary.avgMinutesPerDay.toHoursLabel(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant)
                                        )
                                    }

                                    // Table
                                    ShopLogsTableHeader()

                                    for (day in shopDaySummaries) {
                                        ShopLogsTableRow(
                                            day = day,
                                            onShare = {},
                                            viewRef = {}
                                        )
                                    }
                                }
                            }
                        }
                        }
                    },
                    update = { view ->
                        summaryViewRef.value = view
                        (view as? ComposeView)?.setContent {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Header row with title and action buttons
                                    Row(
                                        modifier              = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text  = summary?.shopName ?: "Shop",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text  = selectedMonth.displayName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = {
                                            if (summary != null) {
                                                PdfExportUtil.generateAndShareShopLogsPdf(
                                                    context,
                                                    summary.shopName,
                                                    summary.monthDisplay,
                                                    shopDaySummaries,
                                                    summary
                                                )
                                            }
                                        }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Share, contentDescription = "Share",
                                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    // Stats row
                                    if (summary != null) {
                                        Row(
                                            modifier              = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text  = "Days Open:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text  = "${summary.totalDaysOpen}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text  = "Total:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text  = summary.totalMinutes.toHoursLabel(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text  = "Avg:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text  = summary.avgMinutesPerDay.toHoursLabel(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant)
                                        )
                                    }

                                    // Table
                                    ShopLogsTableHeader()

                                    for (day in shopDaySummaries) {
                                        ShopLogsTableRow(
                                            day = day,
                                            onShare = {},
                                            viewRef = {}
                                        )
                                    }
                                }
                            }
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
                        contentPadding      = PaddingValues(vertical = 8.dp, horizontal = 0.dp)
                    ) {
                        // Single unified card with summary and records
                        item(key = "emp_unified_card") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Header row with title and action buttons
                                    Row(
                                        modifier              = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text  = summary?.employeeName ?: "Employee",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text  = selectedMonth.displayName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = {
                                            if (summary != null) {
                                                PdfExportUtil.generateAndShareEmployeeLogsPdf(
                                                    context,
                                                    summary.employeeName,
                                                    summary.shopName,
                                                    summary.monthDisplay,
                                                    employeeDayRecords,
                                                    summary
                                                )
                                            }
                                        }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Share, contentDescription = "Share",
                                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    // Stats row
                                    if (summary != null) {
                                        Row(
                                            modifier              = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text  = "Days Worked:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text  = "${summary.totalDays}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text  = "Total:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text  = summary.totalMinutes.toHoursLabel(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text  = "Avg:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text  = summary.avgMinutesPerDay.toHoursLabel(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant)
                                        )
                                    }

                                    // Table
                                    ShopLogsTableHeader()

                                    for (day in employeeDayRecords) {
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
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ── Shop Logs Table ───────────────────────────────────────────────────────────

@Composable
private fun ShopLogsTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Date", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
        Text("In", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("Out", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("Dur", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
        Text("Total", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
    }
}

@Composable
private fun ShopLogsTableRow(
    day: DayLogSummary,
    onShare: () -> Unit,
    viewRef: (android.view.View) -> Unit
) {
    val isOpen = day.sessions.isNotEmpty()
    val backgroundColor = if (isOpen) MaterialTheme.colorScheme.surface
                         else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)

    // Extract date number (01, 02, etc)
    val dateNumber = try {
        day.date.split("-")[2]
    } catch (e: Exception) {
        "01"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isOpen && day.sessions.isNotEmpty()) {
            // Multiple sessions or single session
            for (index in day.sessions.indices) {
                val session = day.sessions[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show date only on first row
                    if (index == 0) {
                        Text(
                            text = dateNumber,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.width(28.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(28.dp))
                    }

                    Text(
                        text = formatLogTime(session.openTime),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = if (session.closeTime != null) formatLogTime(session.closeTime)
                               else "--",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = if (session.closeTime == null) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Individual session duration
                    Text(
                        text = session.durationMinutes.toHoursLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(0.8f),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Show day total only on first row
                    if (index == 0) {
                        Text(
                            text = day.totalMinutes.toHoursLabel(),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(0.8f))
                    }
                }
            }
        } else {
            // Closed day
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateNumber,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.width(40.dp)
                )
                Text(
                    text = "Closed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(3f)
                )
            }
        }

        // Divider between dates
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
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

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Spacer(modifier = Modifier.height(8.dp))

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

// ── Employee Day Card ─────────────────────────────────────────────────────────

@Composable
private fun EmployeeDayCard(day: EmployeeDayRecord, onShare: () -> Unit) {
    // Extract date number (01, 02, etc)
    val dateNumber = try {
        day.date.split("-")[2]
    } catch (e: Exception) {
        "01"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (day.sessions.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateNumber,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.width(40.dp)
                )
                Text(
                    text = "No record",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(3f)
                )
            }
        } else {
            // Multiple sessions or single session
            for (index in day.sessions.indices) {
                val session = day.sessions[index]
                val backgroundColor = MaterialTheme.colorScheme.surface

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show date only on first row
                    if (index == 0) {
                        Text(
                            text = dateNumber,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.width(28.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(28.dp))
                    }

                    Text(
                        text = formatLogTime(session.loginTime),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = if (session.logoutTime != null) formatLogTime(session.logoutTime)
                               else "--",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = if (session.logoutTime == null) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Individual session duration
                    Text(
                        text = session.durationMinutes.toHoursLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(0.8f),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Show day total only on first row
                    if (index == 0) {
                        Text(
                            text = day.totalMinutes.toHoursLabel(),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(0.8f))
                    }
                }
            }
        }

        // Divider between dates
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

// ── Utilities ─────────────────────────────────────────────────────────────────

fun formatLogDate(dateStr: String): String {
    return try {
        LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy, EEEE"))
    } catch (e: Exception) { dateStr }
}

fun formatLogTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    return try { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp)) }
    catch (e: Exception) { "" }
}
