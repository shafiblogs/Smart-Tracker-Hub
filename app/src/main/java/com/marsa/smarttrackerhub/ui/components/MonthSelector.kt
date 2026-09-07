package com.marsa.smarttrackerhub.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.marsa.smarttrackerhub.domain.MonthOption
import com.marsa.smarttrackerhub.domain.MonthSelection
import java.time.YearMonth

private const val PICK_MONTH_LABEL = "Pick a month…"

/**
 * Month/period field used by every screen with a month selector.
 *
 * With [presets] supplied (Home: current/relative months + trailing ranges), this looks and
 * behaves like [DropdownField] — a menu of the presets plus a "Pick a month…" entry that opens
 * [MonthGridPicker]. With an empty [presets] list (Sales Detail, Account Detail, Logs — screens
 * that only ever want a single month and have no reason to offer a flat list of every month
 * ever logged), there's no menu at all: the field just shows the current selection, and tapping
 * it opens the grid picker directly.
 *
 * [availableMonthKeys] dims months with no data in the grid picker — leave null when there's
 * nothing to check against yet (e.g. Home, until its aggregation math can resolve an arbitrary
 * month — see the D-series follow-up).
 */
@Composable
fun MonthSelector(
    label: String,
    selection: MonthSelection,
    presets: List<MonthSelection>,
    onSelectionChange: (MonthSelection) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    allowCustomMonth: Boolean = true,
    availableMonthKeys: Set<YearMonth>? = null,
    earliestMonth: YearMonth? = null
) {
    var showPicker by remember { mutableStateOf(false) }

    if (presets.isEmpty()) {
        DirectPickerField(
            label = label,
            value = selection.displayName,
            enabled = enabled,
            expanded = showPicker,
            onClick = { showPicker = true },
            modifier = modifier
        )
    } else {
        val optionLabels = remember(presets, allowCustomMonth) {
            presets.map { it.displayName } + if (allowCustomMonth) listOf(PICK_MONTH_LABEL) else emptyList()
        }
        DropdownField(
            label = label,
            selectedValue = selection.displayName,
            options = optionLabels,
            onOptionSelected = { picked ->
                if (picked == PICK_MONTH_LABEL) {
                    showPicker = true
                } else {
                    presets.firstOrNull { it.displayName == picked }?.let(onSelectionChange)
                }
            },
            enabled = enabled,
            modifier = modifier
        )
    }

    if (showPicker) {
        val current = (selection as? MonthSelection.Month)?.option?.yearMonth ?: YearMonth.now()
        MonthGridPicker(
            initialMonth = current,
            availableMonthKeys = availableMonthKeys,
            earliestMonth = earliestMonth,
            onMonthSelected = { picked ->
                onSelectionChange(MonthSelection.Month(MonthOption(picked)))
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

/**
 * A field with [DropdownField]'s exact chrome, but no menu of its own — the whole field is one
 * tap target that runs [onClick] (opening a picker sheet) instead of expanding an option list.
 * `readOnly` alone still lets the text field take focus/cursor, so a fully transparent overlay
 * intercepts the tap before it reaches the field — the standard pattern for date/month pickers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectPickerField(
    label: String,
    value: String,
    enabled: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClick = onClick)
            )
        }
    }
}
