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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttracker.ui.theme.sTypography
import com.marsa.smarttrackerhub.ui.components.CommonTextField
import com.marsa.smarttrackerhub.utils.ShareUtil
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale


/**
 * Created by Muhammed Shafi on 17/03/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 *
 * Logs screen — shows current-month shop-activity events (employee login/logout,
 * shop open/close) from SmartTracker Firebase, grouped as day-summary cards.
 * Each card has its own Share button that captures only that card as an image.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(viewModel: LogsViewModel = viewModel()) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initDatabase(context)
    }

    val shopList     by viewModel.shopList.collectAsState()
    val selectedShop by viewModel.selectedShop.collectAsState()
    val daySummaries by viewModel.daySummaries.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()

    var dropdownExpanded by remember { mutableStateOf(false) }
    val selectedShopName = selectedShop?.shopName ?: "Select a shop"

    // View reference map keyed by date string — used for per-card sharing
    val cardViewRefs = remember { mutableMapOf<String, android.view.View>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // ── Shop dropdown ──────────────────────────────────────────────────
        ExposedDropdownMenuBox(
            expanded         = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = it }
        ) {
            OutlinedTextField(
                value         = selectedShopName,
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Shop") },
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                modifier      = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded         = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                shopList.forEach { shop ->
                    DropdownMenuItem(
                        text    = { Text(shop.shopName) },
                        onClick = {
                            viewModel.selectShop(shop)
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            selectedShop == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CommonTextField(
                        value = "Select a shop to view logs",
                        style = sTypography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium, fontSize = 16.sp
                        )
                    )
                }
            }

            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            daySummaries.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CommonTextField(
                        value = "No log entries for this month. Tap ↺ to refresh.",
                        style = sTypography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium, fontSize = 16.sp
                        )
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(daySummaries, key = { it.date }) { summary ->
                        // Wrap in AndroidView / ComposeView so we can capture a View reference
                        // for per-card image sharing — same pattern as SaleScreen.
                        AndroidView(
                            factory = { ctx ->
                                ComposeView(ctx).apply {
                                    setContent {
                                        DayLogCard(
                                            summary = summary,
                                            onShare = {
                                                cardViewRefs[summary.date]?.let { view ->
                                                    ShareUtil.shareViewAsImage(
                                                        view       = view,
                                                        context    = ctx,
                                                        fileName   = "log_${summary.date}_${selectedShop?.shopName?.replace(" ", "_")}.png",
                                                        shareTitle = "Share Log — ${formatLogDate(summary.date)}"
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            },
                            update = { view ->
                                // Always store the latest view ref for this date
                                cardViewRefs[summary.date] = view
                                (view as? ComposeView)?.setContent {
                                    DayLogCard(
                                        summary = summary,
                                        onShare = {
                                            ShareUtil.shareViewAsImage(
                                                view       = view,
                                                context    = context,
                                                fileName   = "log_${summary.date}_${selectedShop?.shopName?.replace(" ", "_")}.png",
                                                shareTitle = "Share Log — ${formatLogDate(summary.date)}"
                                            )
                                        }
                                    )
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

// ── Day Log Card ──────────────────────────────────────────────────────────────

@Composable
private fun DayLogCard(
    summary: DayLogSummary,
    onShare: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {

            // ── Date header row with share button ──────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = formatLogDate(summary.date),
                    style    = sTypography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color    = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick  = onShare,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Share,
                        contentDescription = "Share this day's log",
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Shop section ───────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🏪", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text  = "Shop",
                    style = sTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            if (summary.shopOpenTime == null) {
                Text(
                    text  = "Not opened today",
                    style = sTypography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TimeChip(label = "Open",  time = summary.shopOpenTime,  color = Color(0xFF22C55E))
                    if (summary.shopCloseTime != null) {
                        Text("→", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TimeChip(label = "Close", time = summary.shopCloseTime, color = Color(0xFFEF4444))
                    } else {
                        Text(
                            text  = "Still open",
                            style = sTypography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF22C55E)
                        )
                    }
                    summary.shopTotalMinutes?.let { mins ->
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text  = mins.toHoursLabel(),
                            style = sTypography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier  = Modifier.padding(vertical = 8.dp),
                color     = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )

            // ── Employees section ──────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👥", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text  = "Employees",
                    style = sTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            if (summary.employees.isEmpty()) {
                Text(
                    text  = "No employees logged in",
                    style = sTypography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                summary.employees.forEach { emp ->
                    EmployeeSessionRow(emp = emp)
                }
            }
        }
    }
}

@Composable
private fun EmployeeSessionRow(emp: EmployeeLogSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text  = emp.employeeName,
            style = sTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (emp.sessions.isEmpty()) {
            Text(
                text     = "No login recorded",
                style    = sTypography.bodySmall.copy(fontSize = 11.sp),
                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 2.dp)
            )
        } else {
            emp.sessions.forEachIndexed { index, session ->
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (emp.sessions.size > 1) {
                        Text(
                            text     = "S${index + 1} ",
                            style    = sTypography.bodySmall.copy(fontSize = 10.sp),
                            color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.width(20.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(20.dp))
                    }
                    Text(
                        text  = formatLogTime(session.loginTime),
                        style = sTypography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF22C55E)
                    )
                    Text(
                        text  = " → ",
                        style = sTypography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text  = if (session.logoutTime != null) formatLogTime(session.logoutTime)
                                else "Working",
                        style = sTypography.bodySmall.copy(fontSize = 11.sp),
                        color = if (session.logoutTime == null) Color(0xFFF97316)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (session.durationMinutes > 0) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text  = session.durationMinutes.toHoursLabel(),
                            style = sTypography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (emp.sessions.size > 1 && emp.totalMinutes > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 3.dp)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text  = "Total: ${emp.totalMinutes.toHoursLabel()}",
                        style = sTypography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeChip(label: String, time: Long, color: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text  = label,
            style = sTypography.bodySmall.copy(fontSize = 9.sp),
            color = color.copy(alpha = 0.7f)
        )
        Text(
            text  = formatLogTime(time),
            style = sTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
            color = color
        )
    }
}

// ── Utilities ─────────────────────────────────────────────────────────────────

internal fun formatLogDate(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        date.format(DateTimeFormatter.ofPattern("dd MMM yyyy, EEEE"))
    } catch (e: Exception) { dateStr }
}

private fun formatLogTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    return try {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
    } catch (e: Exception) { "" }
}

private fun Long.toHoursLabel(): String {
    val h = this / 60
    val m = this % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
