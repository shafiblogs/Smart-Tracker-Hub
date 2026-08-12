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

/**
 * Purchase achievement vs budget: thresholds lowered 10pp (matches SmartTracker/AccountsTracker).
 * Capped at 120% on the top end — "purchase target" is a spending ceiling, so a large overrun
 * is a problem, not an achievement to celebrate in green. Below 90% renders the same as before.
 */
fun purchaseAchievementColor(percentage: Double, status: SemanticStatusColors) =
    when {
        percentage > 120 -> status.danger
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

/**
 * Purchase's achievement bar means the opposite of Sales' — a fuller bar is MORE spend, i.e.
 * worse — so state it as budget variance instead of achievement: "87%" would read as 87% of
 * the way to a goal worth reaching. Shared by the Home card and Sales detail screen (D7) so
 * the same month says the same thing on both.
 */
fun purchaseVarianceText(achievementPercentage: Double, hasBudget: Boolean): String {
    if (!hasBudget) return "—"
    val variancePct = achievementPercentage - 100.0
    return when {
        variancePct == 0.0 -> "on budget"
        variancePct > 0 -> "+${"%.0f".format(variancePct)}% over budget"
        else -> "${"%.0f".format(variancePct)}% under budget"
    }
}

/** Colour for [purchaseVarianceText]: over budget is danger, under/on budget is success. */
fun purchaseVarianceColor(achievementPercentage: Double, hasBudget: Boolean, status: SemanticStatusColors) =
    when {
        !hasBudget -> null
        achievementPercentage > 100.0 -> status.danger
        else -> status.success
    }
