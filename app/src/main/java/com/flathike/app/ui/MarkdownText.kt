package com.flathike.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Lightweight, zero-dependency Markdown renderer for Gemma AI responses in Compose.
 * Handles headings (#, ##, ###), bold (**text**), italic (*text*), code (`code`),
 * bullet points (*, -, •), numbered lists (1., 2.), and dividers.
 */
@Composable
fun FormattedMarkdown(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val lines = text.lines()
    Column(modifier = modifier.fillMaxWidth()) {
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                trimmed.startsWith("### ") -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = parseMarkdownInline(trimmed.removePrefix("### "), MaterialTheme.colorScheme.primary),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                trimmed.startsWith("## ") -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = parseMarkdownInline(trimmed.removePrefix("## "), MaterialTheme.colorScheme.primary),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }
                trimmed.startsWith("# ") -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = parseMarkdownInline(trimmed.removePrefix("# "), MaterialTheme.colorScheme.primary),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                trimmed == "---" || trimmed == "***" -> {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(6.dp))
                }
                trimmed.startsWith("• ") || trimmed.startsWith("* ") || trimmed.startsWith("- ") -> {
                    val bulletContent = when {
                        trimmed.startsWith("• ") -> trimmed.removePrefix("• ")
                        trimmed.startsWith("* ") -> trimmed.removePrefix("* ")
                        else -> trimmed.removePrefix("- ")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 8.dp)
                                .size(5.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Text(
                            text = parseMarkdownInline(bulletContent, textColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                trimmed.matches(Regex("""^\d+\.\s+.*""")) -> {
                    val match = Regex("""^(\d+)\.\s+(.*)""").find(trimmed)
                    val num = match?.groupValues?.get(1) ?: "1"
                    val itemContent = match?.groupValues?.get(2) ?: trimmed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "$num.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(22.dp)
                        )
                        Text(
                            text = parseMarkdownInline(itemContent, textColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                else -> {
                    Text(
                        text = parseMarkdownInline(trimmed, textColor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

/**
 * Parses inline markdown: **bold**, *italic*, `code`.
 */
fun parseMarkdownInline(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val len = text.length

        while (cursor < len) {
            // Check for bold **...**
            if (cursor + 1 < len && text[cursor] == '*' && text[cursor + 1] == '*') {
                val endBold = text.indexOf("**", cursor + 2)
                if (endBold != -1) {
                    val boldContent = text.substring(cursor + 2, endBold)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(boldContent)
                    }
                    cursor = endBold + 2
                    continue
                }
            }

            // Check for inline code `...`
            if (text[cursor] == '`') {
                val endCode = text.indexOf('`', cursor + 1)
                if (endCode != -1) {
                    val codeContent = text.substring(cursor + 1, endCode)
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(codeContent)
                    }
                    cursor = endCode + 1
                    continue
                }
            }

            // Check for italic *...*
            if (text[cursor] == '*' && (cursor + 1 >= len || text[cursor + 1] != '*')) {
                val endItalic = text.indexOf('*', cursor + 1)
                if (endItalic != -1 && (endItalic + 1 >= len || text[endItalic + 1] != '*')) {
                    val italicContent = text.substring(cursor + 1, endItalic)
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(italicContent)
                    }
                    cursor = endItalic + 1
                    continue
                }
            }

            append(text[cursor])
            cursor++
        }
    }
}
