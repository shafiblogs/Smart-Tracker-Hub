package com.marsa.smarttrackerhub.ui.screens.chart

import com.marsa.smarttracker.ui.theme.SemanticStatusColors

/**
 * Achievement and margin colours — a unified vocabulary backed by SemanticStatusColors,
 * so the whole app tints "progress" and "health" the same way.
 */

/** Sales achievement vs target (target = 100%): >=100 success, 90-100 warning, else danger. */
fun salesAchievementColor(percentage: Double, status: SemanticStatusColors) =
    when {
        percentage >= 100 -> status.success
        percentage >= 90 -> status.warning
        else -> status.danger
    }

/** Purchase achievement vs budget: thresholds lowered 10pp (matches SmartTracker/AccountsTracker). */
fun purchaseAchievementColor(percentage: Double, status: SemanticStatusColors) =
    when {
        percentage >= 90 -> status.success
        percentage >= 75 -> status.warning
        else -> status.danger
    }

/** Sales margin health (matches home UnifiedStatisticsCard): >=30 success, 10-30 warning, else danger. */
fun salesMarginColor(percentage: Double, status: SemanticStatusColors) =
    when {
        percentage >= 30 -> status.success
        percentage >= 10 -> status.warning
        else -> status.danger
    }
