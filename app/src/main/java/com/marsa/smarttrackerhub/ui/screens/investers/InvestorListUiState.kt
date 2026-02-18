package com.marsa.smarttrackerhub.ui.screens.investers

import com.marsa.smarttrackerhub.data.entity.InvestorInfo


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
data class InvestorListUiState(
    val investors: List<InvestorInfo> = emptyList(),
    // investorId → Pair(totalInvested, shopCount)
    val portfolioTotals: Map<Int, Pair<Double, Int>> = emptyMap(),
    val isLoading: Boolean = false
)