package com.proot.cowork.ui.kimi

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

@Composable
fun KimiMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = KimiTokens.TextPrimary,
    showCursor: Boolean = false,
) {
    val annotated = remember(text, showCursor) {
        buildKimiAnnotatedString(text, showCursor)
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        color = color,
        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
    )
}

private fun buildKimiAnnotatedString(raw: String, showCursor: Boolean): AnnotatedString =
    buildAnnotatedString {
        var i = 0
        while (i < raw.length) {
            when {
                raw.startsWith("**", i) -> {
                    val end = raw.indexOf("**", i + 2)
                    if (end > i) {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = KimiTokens.TextPrimary)) {
                            append(raw.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(raw[i])
                        i++
                    }
                }
                raw.startsWith("`", i) -> {
                    val end = raw.indexOf('`', i + 1)
                    if (end > i) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                color = KimiTokens.Accent,
                                background = KimiTokens.Card,
                            ),
                        ) {
                            append(raw.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(raw[i])
                        i++
                    }
                }
                raw.startsWith("### ", i) -> {
                    val lineEnd = raw.indexOf('\n', i).let { if (it < 0) raw.length else it }
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = KimiTokens.TextPrimary)) {
                        append(raw.substring(i + 4, lineEnd))
                    }
                    if (lineEnd < raw.length) append('\n')
                    i = lineEnd + 1
                }
                raw.startsWith("## ", i) -> {
                    val lineEnd = raw.indexOf('\n', i).let { if (it < 0) raw.length else it }
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = KimiTokens.TextPrimary)) {
                        append(raw.substring(i + 3, lineEnd))
                    }
                    if (lineEnd < raw.length) append('\n')
                    i = lineEnd + 1
                }
                raw.startsWith("- ", i) || raw.startsWith("• ", i) -> {
                    withStyle(SpanStyle(color = KimiTokens.Accent)) { append("• ") }
                    i += 2
                }
                else -> {
                    append(raw[i])
                    i++
                }
            }
        }
        if (showCursor) {
            withStyle(SpanStyle(color = KimiTokens.Accent)) { append("▍") }
        }
    }
