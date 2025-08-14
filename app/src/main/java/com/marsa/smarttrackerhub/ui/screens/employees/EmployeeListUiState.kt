package com.marsa.smarttrackerhub.ui.screens.employees

import com.marsa.smarttrackerhub.data.entity.EmployeeInfo


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

data class EmployeeListUiState(
    val employees: List<EmployeeInfo> = emptyList(),
    val isLoading: Boolean = false
)