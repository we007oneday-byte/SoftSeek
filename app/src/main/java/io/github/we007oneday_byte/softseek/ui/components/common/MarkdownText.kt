package io.github.we007oneday_byte.softseek.ui.components.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import io.github.we007oneday_byte.softseek.core.settings.CodeTheme
import io.github.we007oneday_byte.softseek.ui.theme.LocalSoftSeekUiSettings
import io.github.we007oneday_byte.softseek.ui.theme.softSeekColors

private sealed interface MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock

    data class Heading(val text: String, val level: Int) : MarkdownBlock

    data class Bullet(val items: List<String>) : MarkdownBlock

    data class Code(val code: String, val language: String?) : MarkdownBlock
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(text) { parseMarkdown(text) }
    val codeColor = MaterialTheme.colorScheme.primary
    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEachIndexed { index, block ->
            if (index > 0) Spacer(Modifier.height(8.dp))
            when (block) {
                is MarkdownBlock.Paragraph -> Text(
                    text = inlineMarkdown(block.text, codeColor),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.softSeekColors.textPrimary,
                )
                is MarkdownBlock.Heading -> Text(
                    text = inlineMarkdown(block.text, codeColor),
                    style = if (block.level <= 2) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.titleSmall
                    },
                    color = MaterialTheme.softSeekColors.textPrimary,
                )
                is MarkdownBlock.Bullet -> Column {
                    block.items.forEach { item ->
                        Row(modifier = Modifier.padding(vertical = 1.dp)) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = inlineMarkdown(item, codeColor),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.softSeekColors.textPrimary,
                            )
                        }
                    }
                }
                is MarkdownBlock.Code -> CodeBlock(block)
            }
        }
    }
}

@Composable
private fun CodeBlock(block: MarkdownBlock.Code) {
    val uiSettings = LocalSoftSeekUiSettings.current
    val darkCode = when (uiSettings.codeTheme) {
        CodeTheme.FOLLOW_THEME -> isSystemInDarkTheme()
        CodeTheme.LIGHT -> false
        CodeTheme.DARK -> true
    }
    val background = if (darkCode) Color(0xFF1B1F24) else Color(0xFFF4F6F8)
    val foreground = if (darkCode) Color(0xFFE6EDF3) else Color(0xFF24292F)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = background,
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            block.language?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (darkCode) Color(0xFF8B949E) else Color(0xFF6E7781),
                )
                Spacer(Modifier.height(6.dp))
            }
            SelectionContainer {
                Text(
                    text = block.code,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = foreground,
                    softWrap = false,
                )
            }
        }
    }
}

private fun parseMarkdown(raw: String): List<MarkdownBlock> {
    val lines = raw.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var index = 0
    while (index < lines.size) {
        val line = lines[index]
        when {
            line.trimStart().startsWith("```") -> {
                val language = line.trim().removePrefix("```").trim().ifBlank { null }
                val code = StringBuilder()
                index++
                while (index < lines.size && !lines[index].trimStart().startsWith("```")) {
                    if (code.isNotEmpty()) code.append('\n')
                    code.append(lines[index])
                    index++
                }
                if (index < lines.size) index++
                blocks += MarkdownBlock.Code(code.toString(), language)
            }
            line.isBlank() -> index++
            line.trimStart().startsWith("#") -> {
                val match = HEADING.matchEntire(line.trim())
                if (match != null) {
                    blocks += MarkdownBlock.Heading(
                        text = match.groupValues[2].trim(),
                        level = match.groupValues[1].length,
                    )
                } else {
                    blocks += MarkdownBlock.Paragraph(line)
                }
                index++
            }
            isBullet(line) -> {
                val items = mutableListOf<String>()
                while (index < lines.size && isBullet(lines[index])) {
                    items += lines[index].trimStart().removePrefix("- ").removePrefix("* ").trim()
                    index++
                }
                blocks += MarkdownBlock.Bullet(items)
            }
            else -> {
                val paragraph = mutableListOf<String>()
                while (index < lines.size && !isBlockBoundary(lines[index])) {
                    paragraph += lines[index]
                    index++
                }
                if (paragraph.isNotEmpty()) {
                    blocks += MarkdownBlock.Paragraph(paragraph.joinToString("\n"))
                }
            }
        }
    }
    return blocks
}

private fun isBullet(line: String): Boolean {
    val trimmed = line.trimStart()
    return trimmed.startsWith("- ") || trimmed.startsWith("* ")
}

private fun isBlockBoundary(line: String): Boolean =
    line.isBlank() || line.trimStart().startsWith("```") || line.trimStart().startsWith("#") || isBullet(line)

private fun inlineMarkdown(raw: String, codeColor: Color): AnnotatedString = buildAnnotatedString {
    var index = 0
    while (index < raw.length) {
        when {
            raw.startsWith("**", index) -> {
                val end = raw.indexOf("**", index + 2)
                if (end < 0) {
                    append(raw.substring(index))
                    return@buildAnnotatedString
                }
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append(raw.substring(index + 2, end))
                }
                index = end + 2
            }
            raw[index] == '`' -> {
                val end = raw.indexOf('`', index + 1)
                if (end < 0) {
                    append(raw.substring(index))
                    return@buildAnnotatedString
                }
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = codeColor)) {
                    append(raw.substring(index + 1, end))
                }
                index = end + 1
            }
            raw[index] == '*' -> {
                val end = raw.indexOf('*', index + 1)
                if (end < 0) {
                    append(raw.substring(index))
                    return@buildAnnotatedString
                }
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(raw.substring(index + 1, end))
                }
                index = end + 1
            }
            else -> {
                append(raw[index])
                index++
            }
        }
    }
}

private val HEADING = Regex("^(#{1,6})\\s+(.*)$")
