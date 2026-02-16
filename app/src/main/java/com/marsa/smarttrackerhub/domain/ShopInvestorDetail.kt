package com.marsa.smarttrackerhub.domain

import com.marsa.smarttrackerhub.data.entity.InvestorInfo
import com.marsa.smarttrackerhub.data.entity.ShopInfo

/**
 * Combined data for displaying investor details in shop view
 * Created by Muhammed Shafi on 16/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
data class ShopInvestorDetail(
    val shopInvestorId: Int,
    val investorId: Int,
    val investorName: String,
    val investorEmail: String,
    val investorPhone: String,
    val sharePercentage: Double,
    val investmentAmount: Double,
    val investmentDate: Long
)

/**
 * Combined data for displaying shop details in investor view
 */
data class InvestorShopDetail(
    val shopInvestorId: Int,
    val shopId: Int,
    val shopName: String,
    val shopAddress: String,
    val sharePercentage: Double,
    val investmentAmount: Double,
    val investmentDate: Long
)

/**
 * Financial summary for a shop
 */
data class ShopFinancialSummary(
    val shopInfo: ShopInfo,
    val totalInvestment: Double,
    val investorCount: Int,
    val allocatedPercentage: Double,
    val remainingPercentage: Double
)

/**
 * Portfolio summary for an investor
 */
data class InvestorPortfolio(
    val investorInfo: InvestorInfo,
    val totalInvested: Double,
    val shopCount: Int
)