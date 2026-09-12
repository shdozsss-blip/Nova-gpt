package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NovaBlue
import com.example.ui.theme.NovaCyan

@Composable
fun MarkdownView(
    content: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    baseFontSize: TextUnit = 15.sp
) {
    val blocks = parseMarkdownBlocks(content)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Code -> {
                    CodeBlockView(
                        code = block.code,
                        language = block.language
                    )
                }
                is MarkdownBlock.Heading -> {
                    val headingSize = when (block.level) {
                        1 -> baseFontSize * 1.35f
                        2 -> baseFontSize * 1.2f
                        else -> baseFontSize * 1.1f
                    }
                    Text(
                        text = block.text,
                        color = textColor,
                        fontSize = headingSize,
                        fontWeight = FontWeight.Bold,
                        lineHeight = headingSize * 1.3f,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                is MarkdownBlock.BulletPoint -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "•",
                            color = NovaBlue,
                            fontSize = baseFontSize * 1.1f,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = buildAnnotatedStringWithFormatting(block.text, textColor),
                            color = textColor,
                            fontSize = baseFontSize,
                            lineHeight = baseFontSize * 1.45f
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildAnnotatedStringWithFormatting(block.text, textColor),
                        color = textColor,
                        fontSize = baseFontSize,
                        lineHeight = baseFontSize * 1.45f
                    )
                }
            }
        }
    }
}

sealed class MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock()
    data class Heading(val text: String, val level: Int) : MarkdownBlock()
    data class BulletPoint(val text: String) : MarkdownBlock()
    data class Code(val code: String, val language: String) : MarkdownBlock()
}

fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = rawText.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Check code block start
        if (line.trim().startsWith("```")) {
            val language = line.trim().removePrefix("```").trim()
            val codeBuilder = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeBuilder.append(lines[i]).append("\n")
                i++
            }
            // skip closing ```
            if (i < lines.size) i++
            blocks.add(MarkdownBlock.Code(code = codeBuilder.toString(), language = language))
            continue
        }

        // Check headings
        if (line.startsWith("# ")) {
            blocks.add(MarkdownBlock.Heading(line.removePrefix("# ").trim(), level = 1))
            i++
            continue
        } else if (line.startsWith("## ")) {
            blocks.add(MarkdownBlock.Heading(line.removePrefix("## ").trim(), level = 2))
            i++
            continue
        } else if (line.startsWith("### ")) {
            blocks.add(MarkdownBlock.Heading(line.removePrefix("### ").trim(), level = 3))
            i++
            continue
        }

        // Check bullet points
        val trimmed = line.trim()
        if (trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            val bulletText = trimmed.drop(2).trim()
            blocks.add(MarkdownBlock.BulletPoint(bulletText))
            i++
            continue
        }

        // Paragraph or blank line
        if (trimmed.isNotBlank()) {
            blocks.add(MarkdownBlock.Paragraph(line))
        }
        i++
    }

    return blocks
}

fun buildAnnotatedStringWithFormatting(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val length = text.length

        while (cursor < length) {
            val boldStart = text.indexOf("**", cursor)
            val codeStart = text.indexOf("`", cursor)

            var nextSpecial = -1
            var type = ""

            if (boldStart != -1 && (codeStart == -1 || boldStart < codeStart)) {
                nextSpecial = boldStart
                type = "bold"
            } else if (codeStart != -1) {
                nextSpecial = codeStart
                type = "code"
            }

            if (nextSpecial == -1) {
                // No more special tokens
                append(text.substring(cursor))
                break
            }

            // Append normal text prior to token
            if (nextSpecial > cursor) {
                append(text.substring(cursor, nextSpecial))
            }

            if (type == "bold") {
                val boldEnd = text.indexOf("**", nextSpecial + 2)
                if (boldEnd != -1) {
                    val boldContent = text.substring(nextSpecial + 2, boldEnd)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(boldContent)
                    }
                    cursor = boldEnd + 2
                } else {
                    append("**")
                    cursor = nextSpecial + 2
                }
            } else if (type == "code") {
                val codeEnd = text.indexOf("`", nextSpecial + 1)
                if (codeEnd != -1) {
                    val codeContent = text.substring(nextSpecial + 1, codeEnd)
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x334E75FF),
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(" $codeContent ")
                    }
                    cursor = codeEnd + 1
                } else {
                    append("`")
                    cursor = nextSpecial + 1
                }
            }
        }
    }
}
