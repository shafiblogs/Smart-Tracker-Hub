package com.marsa.smarttrackerhub.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.marsa.smarttrackerhub.domain.AvailableMonth
import com.marsa.smarttrackerhub.domain.MonthOption
import com.marsa.smarttrackerhub.domain.MonthSelection

/**
 * [MonthSelector] specialised for the Sales/Account Detail screens' `AvailableMonth` list.
 * Deliberately offers no flat list of every past month — arriving here already means a month
 * was picked on the list screen, so the field just shows that selection; tapping it opens the
 * grid picker directly (see [MonthSelector]'s empty-presets behaviour). Resolving a pick back to
 * a Firestore document id always prefers the stored [AvailableMonth.id] over a regenerated key,
 * so an existing document is never looked up under a slightly different string.
 */
@Composable
fun AvailableMonthSelector(
    label: String,
    availableMonths: List<AvailableMonth>,
    selectedMonthId: String,
    onMonthIdSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val selectedOption = availableMonths.firstOrNull { it.id == selectedMonthId }?.option
        ?: MonthOption.fromSummaryKey(selectedMonthId)
        ?: MonthOption.current()

    MonthSelector(
        label = label,
        selection = MonthSelection.Month(selectedOption),
        presets = emptyList(),
        onSelectionChange = { sel ->
            if (sel is MonthSelection.Month) {
                val id = availableMonths.firstOrNull { it.option == sel.option }?.id
                    ?: sel.option.yearMonth.format(MonthOption.SUMMARY_KEY_FORMATTER)
                onMonthIdSelected(id)
            }
        },
        availableMonthKeys = availableMonths.mapNotNull { it.yearMonth }.toSet(),
        modifier = modifier,
        enabled = enabled
    )
}
