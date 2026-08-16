package com.example.ui.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun MarkdownCompilerView(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lines = remember(content) { content.lines() }

    // Compute Markdown Stats
    val wordCount = remember(content) {
        content.split("\\s+".toRegex()).count { it.isNotBlank() }
    }
    val charCount = remember(content) { content.length }
    val readingTimeMin = remember(wordCount) {
        (wordCount / 200.0).coerceAtLeast(0.5)
    }

    val totalTasks = remember(lines) {
        lines.count { it.trim().startsWith("- [ ]") || it.trim().startsWith("- [x]") || it.trim().startsWith("- [X]") }
    }
    val completedTasks = remember(lines) {
        lines.count { it.trim().startsWith("- [x]") || it.trim().startsWith("- [X]") }
    }

    fun toggleTask(targetLineIndex: Int) {
        val updated = lines.toMutableList()
        if (targetLineIndex in updated.indices) {
            val line = updated[targetLineIndex]
            val trimmed = line.trim()
            if (trimmed.startsWith("- [ ]")) {
                updated[targetLineIndex] = line.replaceFirst("- [ ]", "- [x]")
            } else if (trimmed.startsWith("- [x]") || trimmed.startsWith("- [X]")) {
                updated[targetLineIndex] = line.replaceFirst(Regex("- \\[x\\]|- \\[X\\]"), "- [ ]")
            }
            onContentChange(updated.joinToString("\n"))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .animateContentSize()
    ) {
        // Markdown Metadata & Compilation Summary Badge Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Compiled Markdown",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "$wordCount words • ${String.format("%.1f", readingTimeMin)} min read",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (totalTasks > 0) {
                        Surface(
                            color = if (completedTasks == totalTasks) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                "$completedTasks/$totalTasks tasks",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (completedTasks == totalTasks) Color(0xFF059669) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        if (totalTasks > 0) {
            val progress = completedTasks.toFloat() / totalTasks.toFloat()
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .padding(bottom = 12.dp),
                color = if (completedTasks == totalTasks) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Render Markdown Blocks
        var inCodeBlock = false
        var codeBlockLanguage = ""
        val codeBlockLines = mutableListOf<String>()

        var inTable = false
        val tableRows = mutableListOf<List<String>>()

        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()

            // Handle Code Blocks ```
            if (trimmed.startsWith("```")) {
                if (!inCodeBlock) {
                    inCodeBlock = true
                    codeBlockLanguage = trimmed.removePrefix("```").trim()
                    codeBlockLines.clear()
                } else {
                    inCodeBlock = false
                    val codeSnippet = codeBlockLines.joinToString("\n")
                    CodeBlockCard(codeSnippet, codeBlockLanguage)
                    Spacer(modifier = Modifier.height(10.dp))
                }
                return@forEachIndexed
            }

            if (inCodeBlock) {
                codeBlockLines.add(line)
                return@forEachIndexed
            }

            // Handle Tables | Col 1 | Col 2 |
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                if (!inTable) {
                    inTable = true
                    tableRows.clear()
                }
                val cols = trimmed.split("|")
                    .map { it.trim() }
                    .filterIndexed { i, _ -> i != 0 && i != trimmed.split("|").lastIndex }
                
                // Skip separator rows like |---|---|
                if (!cols.all { it.matches(Regex("[-:]+")) }) {
                    tableRows.add(cols)
                }
                return@forEachIndexed
            } else if (inTable) {
                inTable = false
                RenderMarkdownTable(tableRows.toList())
                tableRows.clear()
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Handle standard markdown elements
            when {
                // H1
                trimmed.startsWith("# ") -> {
                    Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                        Text(
                            text = trimmed.removePrefix("# ").trim(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 4.dp),
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    }
                }
                // H2
                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.removePrefix("## ").trim(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                    )
                }
                // H3
                trimmed.startsWith("### ") -> {
                    Text(
                        text = trimmed.removePrefix("### ").trim(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                // Callouts > [!NOTE], > [!TIP], > [!WARNING], > [!IMPORTANT]
                trimmed.startsWith("> [!NOTE]") || trimmed.startsWith("> [!TIP]") || trimmed.startsWith("> [!WARNING]") || trimmed.startsWith("> [!IMPORTANT]") -> {
                    val type = when {
                        trimmed.startsWith("> [!TIP]") -> "TIP"
                        trimmed.startsWith("> [!WARNING]") -> "WARNING"
                        trimmed.startsWith("> [!IMPORTANT]") -> "IMPORTANT"
                        else -> "NOTE"
                    }
                    val msg = trimmed.substringAfter("]").trim()
                    CalloutBox(type = type, text = msg)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                // Blockquote >
                trimmed.startsWith("> ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(28.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = trimmed.removePrefix("> ").trim(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                // Interactive Checklists - [ ] and - [x]
                trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") -> {
                    val isChecked = trimmed.startsWith("- [x]") || trimmed.startsWith("- [X]")
                    val taskText = trimmed.substring(6)
                    Surface(
                        onClick = { toggleTask(index) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { toggleTask(index) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = taskText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (isChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
                // Bullet list - or *
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = parseInlineMarkdown(trimmed.substring(2)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                // Numbered list 1. 2.
                trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val number = trimmed.substringBefore(".")
                    val body = trimmed.substringAfter(".").trim()
                    Row(
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "$number.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseInlineMarkdown(body),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                // Horizontal Divider --- or ***
                trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                // Embedded Image ![alt](file://...) or ![alt](http...)
                trimmed.startsWith("![") && trimmed.contains("](") && trimmed.endsWith(")") -> {
                    val alt = trimmed.substringAfter("![").substringBefore("]")
                    val url = trimmed.substringAfter("](").removeSuffix(")")
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column {
                            AsyncImage(
                                model = url,
                                contentDescription = alt,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            if (alt.isNotBlank()) {
                                Text(
                                    text = alt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
                // Regular Paragraph
                else -> {
                    if (trimmed.isNotBlank()) {
                        Text(
                            text = parseInlineMarkdown(trimmed),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Flush remaining table if at the end of file
        if (inTable && tableRows.isNotEmpty()) {
            RenderMarkdownTable(tableRows)
        }
    }
}

@Composable
fun CodeBlockCard(code: String, language: String) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifBlank { "code" }.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8)
                    )
                )
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("code", code)
                        clipboard.setPrimaryClip(clip)
                        copied = true
                        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (copied) Color(0xFF10B981) else Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}

@Composable
fun CalloutBox(type: String, text: String) {
    val (bg, border, icon, title) = when (type) {
        "TIP" -> Quad(Color(0xFFECFDF5), Color(0xFF10B981), Icons.Default.Lightbulb, "Tip")
        "WARNING" -> Quad(Color(0xFFFFFBEB), Color(0xFFF59E0B), Icons.Default.Warning, "Warning")
        "IMPORTANT" -> Quad(Color(0xFFFEF2F2), Color(0xFFEF4444), Icons.Default.PriorityHigh, "Important")
        else -> Quad(Color(0xFFEFF6FF), Color(0xFF3B82F6), Icons.Default.Info, "Note")
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, border.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = border, modifier = Modifier.size(20.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = border, style = MaterialTheme.typography.labelMedium)
                if (text.isNotBlank()) {
                    Text(text, style = MaterialTheme.typography.bodySmall, color = Color(0xFF1F2937))
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun RenderMarkdownTable(rows: List<List<String>>) {
    if (rows.isEmpty()) return

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            rows.forEachIndexed { rowIndex, cols ->
                val isHeader = rowIndex == 0
                Row(
                    modifier = Modifier
                        .background(
                            if (isHeader) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else if (rowIndex % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    cols.forEach { colText ->
                        Text(
                            text = colText,
                            style = if (isHeader) MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            else MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .widthIn(min = 90.dp, max = 220.dp)
                                .padding(end = 12.dp)
                        )
                    }
                }
                if (isHeader) {
                    HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

// Inline Markdown Parser for **bold**, *italic*, `code`, and #tag
fun parseInlineMarkdown(text: String) = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            // Bold **text**
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            // Inline Code `text`
            text.startsWith("`", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x2264748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(" ${text.substring(i + 1, end)} ")
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            // Tag #topic
            text[i] == '#' && (i == 0 || text[i - 1].isWhitespace()) -> {
                val end = text.indexOfAny(charArrayOf(' ', '\n', '\t', ',', '.'), i)
                val tagStr = if (end != -1) text.substring(i, end) else text.substring(i)
                withStyle(SpanStyle(color = Color(0xFF3B82F6), fontWeight = FontWeight.SemiBold)) {
                    append(tagStr)
                }
                i += tagStr.length
            }
            // Italic *text*
            text.startsWith("*", i) -> {
                val end = text.indexOf("*", i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}
