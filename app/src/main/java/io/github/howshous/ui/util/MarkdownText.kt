package io.github.howshous.ui.util

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    // Split by lines to handle bullet lists and tables
    val lines = text.split("\n")

    Column(modifier = modifier.fillMaxWidth()) {
        var i = 0
        while (i < lines.size) {
            val trimmed = lines[i].trim()
            val next = lines.getOrNull(i + 1)?.trim()
            if (isTableHeader(trimmed, next)) {
                val header = parseTableRow(trimmed)
                val rows = mutableListOf<List<String>>()
                i += 2
                while (i < lines.size) {
                    val rowLine = lines[i].trim()
                    if (!looksLikeTableRow(rowLine)) break
                    rows.add(parseTableRow(rowLine))
                    i++
                }
                MarkdownTable(header = header, rows = rows)
                continue
            }

            when {
                trimmed.startsWith("#") -> {
                    val level = trimmed.takeWhile { it == '#' }.length
                    val content = trimmed.drop(level).trim()
                    val style = when (level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }
                    InlineMarkdownText(
                        text = content,
                        style = style,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                isBulletLine(trimmed) -> {
                    val content = trimmed.drop(2).trim()
                    val style = MaterialTheme.typography.bodyMedium
                    Text(
                        text = buildAnnotatedString {
                            append("- ")
                            appendInlineMarkdown(this, content, style)
                        },
                        style = style,
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 2.dp)
                    )
                }
                Regex("^\\d+\\.\\s+.*").matches(trimmed) -> {
                    val number = trimmed.substringBefore(".")
                    val content = trimmed.substringAfter(".").trim()
                    val style = MaterialTheme.typography.bodyMedium
                    Text(
                        text = buildAnnotatedString {
                            append("$number. ")
                            appendInlineMarkdown(this, content, style)
                        },
                        style = style,
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 2.dp)
                    )
                }
                trimmed.isEmpty() -> {
                    Text("", modifier = Modifier.padding(vertical = 4.dp))
                }
                else -> {
                    InlineMarkdownText(
                        text = trimmed,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
            i++
        }
    }
}

@Composable
private fun InlineMarkdownText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val annotated = buildInlineAnnotatedString(text, style)
    Text(text = annotated, style = style, modifier = modifier)
}

private fun buildInlineAnnotatedString(text: String, baseStyle: TextStyle): AnnotatedString {
    return buildAnnotatedString {
        appendInlineMarkdown(this, text, baseStyle)
    }
}

private fun appendInlineMarkdown(builder: AnnotatedString.Builder, text: String, baseStyle: TextStyle) {
    val baseSpan = baseStyle.toSpanStyle()
    val boldSpan = baseStyle.copy(fontWeight = FontWeight.Bold).toSpanStyle()
    val italicSpan = baseStyle.copy(fontStyle = FontStyle.Italic).toSpanStyle()
    val boldItalicSpan = baseStyle.copy(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic).toSpanStyle()

    var bold = false
    var italic = false
    val buffer = StringBuilder()

    fun currentSpan() = when {
        bold && italic -> boldItalicSpan
        bold -> boldSpan
        italic -> italicSpan
        else -> baseSpan
    }

    fun flush() {
        if (buffer.isNotEmpty()) {
            val span = currentSpan()
            builder.withStyle(span) {
                append(buffer.toString())
            }
            buffer.setLength(0)
        }
    }

    var i = 0
    while (i < text.length) {
        if (text.startsWith("**", i)) {
            flush()
            bold = !bold
            i += 2
            continue
        }
        if (text[i] == '*') {
            flush()
            italic = !italic
            i += 1
            continue
        }
        buffer.append(text[i])
        i++
    }
    flush()
}

private fun isBulletLine(line: String): Boolean {
    if (line.startsWith("- ") || line.startsWith("* ")) return true
    if (line.length >= 2 && line[0].code == 8226 && line[1].isWhitespace()) return true
    return false
}

private fun isTableHeader(line: String, nextLine: String?): Boolean {
    if (nextLine == null) return false
    if (!line.contains("|")) return false
    return isTableSeparator(nextLine)
}

private fun isTableSeparator(line: String): Boolean {
    if (line.isBlank()) return false
    val trimmed = line.trim()
    if (!trimmed.contains("|")) return false
    return trimmed.all { it == '|' || it == '-' || it == ':' || it.isWhitespace() }
}

private fun looksLikeTableRow(line: String): Boolean {
    return line.contains("|") && !isTableSeparator(line)
}

private fun parseTableRow(line: String): List<String> {
    val trimmed = line.trim().trim('|')
    if (trimmed.isEmpty()) return emptyList()
    return trimmed.split("|").map { it.trim() }
}

@Composable
private fun MarkdownTable(header: List<String>, rows: List<List<String>>) {
    val columnCount = maxOf(header.size, rows.maxOfOrNull { it.size } ?: 0)
    if (columnCount == 0) return

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        TableRow(cells = header, columnCount = columnCount, isHeader = true)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        rows.forEach { row ->
            TableRow(cells = row, columnCount = columnCount, isHeader = false)
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, columnCount: Int, isHeader: Boolean) {
    val style = if (isHeader) {
        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.bodyMedium
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        for (index in 0 until columnCount) {
            val text = cells.getOrNull(index).orEmpty()
            Text(
                text = buildInlineAnnotatedString(text, style),
                style = style,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
