package com.marsa.smarttrackerhub.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import com.marsa.smarttracker.ui.theme.DIRHAM_GLYPH
import com.marsa.smarttracker.ui.theme.UAESymbolFontFamily
import com.marsa.smarttrackerhub.utils.formatMoney

/**
 * Appends the AED glyph (in the UAESymbol font) followed by a space and the formatted numeral —
 * the Compose equivalent of the web design system's `<Aed>` component. [baseFontSize] sizes the
 * glyph relative to the surrounding text: the glyph reads optically heavier than Latin digits at
 * equal size, so it renders at 0.92× that size, matching the web component.
 */
fun AnnotatedString.Builder.appendAed(
    amount: Double,
    decimals: Int = 0,
    baseFontSize: TextUnit = TextUnit.Unspecified
) {
    val glyphSize = if (baseFontSize.type == TextUnitType.Sp)
        (baseFontSize.value * 0.92f).sp
    else
        TextUnit.Unspecified

    withStyle(SpanStyle(fontFamily = UAESymbolFontFamily, fontSize = glyphSize)) {
        append(DIRHAM_GLYPH)
    }
    append(" ")
    withStyle(SpanStyle(fontFeatureSettings = "tnum")) {
        append(formatMoney(amount, decimals))
    }
}

/**
 * A bare AED amount: "ê 1,250,000". Use wherever a monetary figure is shown in the UI — the
 * glyph is a single character, so (unlike the old "AED " text prefix) it carries no per-row
 * noise penalty and belongs on every amount, not just card headlines.
 *
 * [prefix]/[suffix] let the amount sit inside a short compound phrase ("of ê 500,000 target",
 * "+ê 196,000 retained") without breaking tabular-nums or the glyph's mixed-font span.
 */
@Composable
fun AedText(
    amount: Double,
    decimals: Int = 0,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
    prefix: String = "",
    suffix: String = ""
) {
    val text = buildAnnotatedString {
        if (prefix.isNotEmpty()) append(prefix)
        appendAed(amount, decimals, style.fontSize)
        if (suffix.isNotEmpty()) append(suffix)
    }
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign
    )
}

/** Non-composable variant for call sites that build their own [AnnotatedString] (e.g. MetricCell). */
fun aedAnnotatedString(amount: Double, decimals: Int = 0): AnnotatedString = buildAnnotatedString {
    appendAed(amount, decimals)
}
