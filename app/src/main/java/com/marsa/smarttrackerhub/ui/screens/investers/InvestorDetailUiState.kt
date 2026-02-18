package com.marsa.smarttrackerhub.ui.screens.investers

import com.marsa.smarttrackerhub.data.entity.InvestorInfo
import com.marsa.smarttrackerhub.domain.InvestorShopDetail

/**
 * Created by Muhammed Shafi on 17/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
data class InvestorDetailUiState(
    val investor: InvestorInfo? = null,
    val shopInvestments: List<InvestorShopDetail> = emptyList(),
    val totalInvested: Double = 0.0,
    val shopCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)
