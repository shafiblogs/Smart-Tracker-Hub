package com.marsa.smarttrackerhub.ui.screens.investers

import com.marsa.smarttrackerhub.data.entity.InvestorInfo
import com.marsa.smarttrackerhub.domain.InvestorShopSummary

/**
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
data class InvestorDetailUiState(
    val investor: InvestorInfo? = null,
    val shopSummaries: List<InvestorShopSummary> = emptyList(),
    val totalPaidAllShops: Double = 0.0,
    val activeShopCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)
