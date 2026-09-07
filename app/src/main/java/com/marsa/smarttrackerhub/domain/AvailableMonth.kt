package com.marsa.smarttrackerhub.domain

import java.time.YearMonth

/**
 * One month that actually has a Firestore summary document, from a shop's
 * `summary/{shopId}/months` collection (sales or account — both use the same convention).
 *
 * [id] is the exact document id / Room `monthYear` key — every existing `selectMonth(id)` call
 * and Firestore `.document(id)` lookup already depends on this being that literal string, so it
 * stays authoritative rather than being regenerated from [option]. [option] is the parsed
 * calendar identity, null if [id] doesn't parse as "MonthName - yyyy" (defensive only — nothing
 * this app writes should fail to parse, but a Firestore listener must not crash on a malformed
 * document id).
 */
data class AvailableMonth(val id: String) {
    val option: MonthOption? = MonthOption.fromSummaryKey(id)
    val yearMonth: YearMonth? get() = option?.yearMonth
    val displayName: String get() = option?.displayName ?: id
}
