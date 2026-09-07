package com.marsa.smarttracker.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.marsa.smarttrackerhub.R

/**
 * The official UAE Dirham symbol (unveiled by the UAE Central Bank), delivered as a single-glyph
 * font — the character U+00EA renders as the Dirham mark. Same font/glyph convention used by the
 * `<Aed>` component in the web design system (~/design-system/components/Aed.vue).
 *
 * Source: github.com/abdulrysrr/new-dirham-symbol (MIT licence).
 */
val UAESymbolFontFamily = FontFamily(Font(R.font.dirham_symbol))

/** The Dirham glyph character. Render it in [UAESymbolFontFamily] — any other font shows "ê". */
const val DIRHAM_GLYPH = "ê"
