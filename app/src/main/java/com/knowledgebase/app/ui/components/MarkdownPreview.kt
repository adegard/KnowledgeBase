package com.knowledgebase.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pure-Compose Markdown preview. No WebView, no JavaScript, no network.
 * Supports common block/inline syntax plus links, tables and local images.
 */
@Composable
fun MarkdownPreview(
    content: String,
    fontSize: Float,
    modifier: Modifier = Modifier,
    baseDir: String? = null
) {
    val baseTextStyle = MaterialTheme.typography.bodyMedium
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val uriHandler = LocalUriHandler.current
    val blocks = remember(content, baseDir) { parseBlocks(content) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (block in blocks) {
            when (block) {
                is Block.Header -> Text(
                    inlineMarkdown(block.text) { uriHandler.openUri(it) },
                    fontSize = (fontSize + (6 - block.level.coerceIn(1, 6)) * 2).coerceAtLeast(14f).sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )
                is Block.Paragraph -> Text(
                    inlineMarkdown(block.text) { uriHandler.openUri(it) },
                    style = baseTextStyle.copy(fontSize = fontSize.sp),
                    color = onSurface
                )
                is Block.Code -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceVariant, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    block.lines.forEach { line ->
                        Text(
                            line.ifEmpty { " " },
                            fontFamily = FontFamily.Monospace,
                            fontSize = (fontSize - 1).coerceAtLeast(10f).sp,
                            color = onSurfaceVariant
                        )
                    }
                }
                is Block.Blockquote -> Text(
                    inlineMarkdown(block.text) { uriHandler.openUri(it) },
                    style = baseTextStyle.copy(fontSize = fontSize.sp),
                    color = onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                )
                is Block.BulletList -> Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    block.items.forEach { item ->
                        Row {
                            Text("•  ", style = baseTextStyle.copy(fontSize = fontSize.sp), color = primary)
                            Text(
                                inlineMarkdown(item) { uriHandler.openUri(it) },
                                style = baseTextStyle.copy(fontSize = fontSize.sp),
                                color = onSurface
                            )
                        }
                    }
                }
                is Block.OrderedList -> Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    block.items.forEachIndexed { i, item ->
                        Row {
                            Text("${i + 1}.  ", style = baseTextStyle.copy(fontSize = fontSize.sp), color = primary)
                            Text(
                                inlineMarkdown(item) { uriHandler.openUri(it) },
                                style = baseTextStyle.copy(fontSize = fontSize.sp),
                                color = onSurface
                            )
                        }
                    }
                }
                is Block.Table -> Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceVariant)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        block.header.forEach { cell ->
                            Text(
                                inlineMarkdown(cell) { uriHandler.openUri(it) },
                                style = baseTextStyle.copy(
                                    fontSize = fontSize.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    HorizontalDivider()
                    block.rows.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            row.forEachIndexed { index, cell ->
                                Text(
                                    inlineMarkdown(cell) { uriHandler.openUri(it) },
                                    style = baseTextStyle.copy(fontSize = fontSize.sp),
                                    color = onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                is Block.Image -> {
                    val bmp = remember(block.path, block.alt, baseDir) {
                        decodeImage(baseDir, block.path)
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = block.alt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 340.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            if (block.alt.isNotEmpty()) block.alt else block.path,
                            style = baseTextStyle.copy(
                                fontSize = fontSize.sp,
                                fontStyle = FontStyle.Italic
                            ),
                            color = onSurfaceVariant
                        )
                    }
                }
                is Block.Hr -> HorizontalDivider(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * Inline markdown -> annotated string with bold / italic / code spans and
 * clickable links. Bare URLs are auto-linked. Runs inside the composable so
 * the resolved theme colors and URI handler are reused.
 */
@Composable
private fun inlineMarkdown(
    text: String,
    onLinkClick: (String) -> Unit
): AnnotatedString {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    val boldSpan = SpanStyle(fontWeight = FontWeight.Bold, color = onSurface)
    val italicSpan = SpanStyle(color = primary)
    val codeSpan = SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = MaterialTheme.colorScheme.surfaceVariant
    )
    val linkStyle = TextLinkStyles(
        style = SpanStyle(color = primary, textDecoration = TextDecoration.Underline),
        pressedStyle = SpanStyle(color = primary.copy(alpha = 0.7f))
    )

    fun needsScan(s: String): Boolean =
        s.contains('*') || s.contains('`') || s.contains('_') ||
            s.contains('[') || s.contains("http")

    if (!needsScan(text)) {
        return buildAnnotatedString { append(text) }
    }

    var sb = StringBuilder()
    data class Op(val style: SpanStyle, val start: Int, val end: Int)
    data class LinkOp(val url: String, val start: Int, val end: Int)
    val ops = mutableListOf<Op>()
    val links = mutableListOf<LinkOp>()

    fun plain(s: String) { sb.append(s) }
    fun styled(s: String, style: SpanStyle) {
        val start = sb.length
        sb.append(s)
        ops.add(Op(style, start, sb.length))
    }
    fun linked(s: String, url: String) {
        val start = sb.length
        sb.append(s)
        links.add(LinkOp(url, start, sb.length))
    }

    var i = 0
    val n = text.length
    while (i < n) {
        val c = text[i]
        when {
            text.startsWith("http", i) -> {
                var j = i
                while (j < n && !text[j].isWhitespace()) j++
                val url = text.substring(i, j)
                linked(url, url)
                i = j
            }
            c == '[' -> {
                val close = text.indexOf(']', i + 1)
                if (close > i && text.getOrNull(close + 1) == '(') {
                    val closeParen = text.indexOf(')', close + 2)
                    if (closeParen > close) {
                        val label = text.substring(i + 1, close)
                        val url = text.substring(close + 2, closeParen).trim()
                        linked(label, url)
                        i = closeParen + 1
                    } else {
                        plain("[")
                        i++
                    }
                } else {
                    plain("[")
                    i++
                }
            }
            c == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i) {
                    styled(text.substring(i + 1, end), codeSpan)
                    i = end + 1
                } else {
                    plain(c.toString()); i++
                }
            }
            c == '*' ->
                if (text.getOrNull(i + 1) == '*') {
                    val end = text.indexOf("**", i + 2)
                    if (end > i) {
                        styled(text.substring(i + 2, end), boldSpan)
                        i = end + 2
                    } else {
                        plain("**"); i += 2
                    }
                } else {
                    val end = text.indexOf('*', i + 1)
                    if (end > i) {
                        styled(text.substring(i + 1, end), italicSpan)
                        i = end + 1
                    } else {
                        plain(c.toString()); i++
                    }
                }
            c == '_' ->
                if (text.getOrNull(i + 1) == '_') {
                    val end = text.indexOf("__", i + 2)
                    if (end > i) {
                        styled(text.substring(i + 2, end), boldSpan)
                        i = end + 2
                    } else {
                        plain("__"); i += 2
                    }
                } else {
                    val end = text.indexOf('_', i + 1)
                    if (end > i) {
                        styled(text.substring(i + 1, end), italicSpan)
                        i = end + 1
                    } else {
                        plain(c.toString()); i++
                    }
                }
            else -> {
                var j = i
                while (j < text.length &&
                    text[j] != '*' && text[j] != '`' &&
                    text[j] != '_' && text[j] != '[' &&
                    !text.startsWith("http", j)
                ) j++
                plain(text.substring(i, j))
                i = j
            }
        }
    }

    val assembled = sb.toString()
    return buildAnnotatedString {
        append(assembled)
        ops.forEach { addStyle(it.style, it.start, it.end) }
        links.forEachIndexed { idx, link ->
            addLink(
                LinkAnnotation.Clickable("l$idx", linkStyle) { onLinkClick(link.url) },
                link.start,
                link.end
            )
        }
    }
}

/**
 * Decode an image referenced by a markdown path. Relative paths are resolved
 * against [baseDir] (the folder holding the note). Huge bitmaps are
 * downsampled to keep memory use sane. Every attempt is logged to
 * /storage/emulated/0/Download/imgdiag.txt so failures can be diagnosed
 * without logcat.
 */
private fun decodeImage(baseDir: String?, path: String): Bitmap? {
    if (path.startsWith("http://") || path.startsWith("https://")) {
        writeImgDiag("http-skipped", baseDir, path, null)
        return null
    }
    var rel = path
    while (rel.startsWith("./")) rel = rel.removePrefix("./")
    val abs = when {
        rel.startsWith("/") -> rel
        else -> baseDir?.let {
            if (it.endsWith('/')) it + rel else "$it/$rel"
        } ?: run {
            writeImgDiag("no-basedir", baseDir, path, null)
            return null
        }
    }
    val f = java.io.File(abs)
    if (!f.exists() || !f.isFile) {
        writeImgDiag("missing", baseDir, path, abs)
        return null
    }
    if (f.length() <= 0) {
        writeImgDiag("empty", baseDir, path, abs)
        return null
    }
    val bounds = BitmapFactory.Options()
    bounds.inJustDecodeBounds = true
    BitmapFactory.decodeFile(abs, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        writeImgDiag("undecodable-bounds", baseDir, path, abs)
        return null
    }
    var sample = 1
    while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) sample *= 2
    val opts = BitmapFactory.Options()
    opts.inSampleSize = sample
    val bmp = try {
        BitmapFactory.decodeFile(abs, opts)
    } catch (t: Throwable) {
        null
    } ?: try {
        java.io.FileInputStream(f).use { stream ->
            val r = BitmapFactory.Options()
            r.inSampleSize = sample
            BitmapFactory.decodeStream(stream, null, r)
        }
    } catch (t: Throwable) {
        null
    }
    writeImgDiag(
        if (bmp != null) "ok" else "decode-null",
        baseDir,
        path,
        "$abs len=${f.length()} w=${bounds.outWidth} h=${bounds.outHeight} sample=$sample"
    )
    return bmp
}

private fun writeImgDiag(status: String, baseDir: String?, path: String, detail: String?) {
    runCatching {
        val f = java.io.File(
            android.os.Environment.getExternalStorageDirectory(),
            "Download/imgdiag.txt"
        )
        f.appendText(
            "$status | baseDir=${baseDir ?: "null"} | path=$path | detail=${detail ?: "-"}\n"
        )
    }
}

private sealed class Block {
    data class Header(val level: Int, val text: String) : Block()
    data class Paragraph(val text: String) : Block()
    data class Code(val lines: List<String>) : Block()
    data class Blockquote(val text: String) : Block()
    data class BulletList(val items: List<String>) : Block()
    data class OrderedList(val items: List<String>) : Block()
    data class Table(val header: List<String>, val rows: List<List<String>>) : Block()
    data class Image(val alt: String, val path: String) : Block()
    object Hr : Block()
}

private val imageLine = Regex("^!\\[([^]]*)]\\((.*?)\\)")

private fun parseBlocks(md: String): List<Block> {
    val blocks = mutableListOf<Block>()
    val lines = md.replace("\r\n", "\n").split("\n")
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val t = line.trim()
        when {
            t.startsWith("```") -> {
                val code = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].contains("```")) {
                    code.add(lines[i])
                    i++
                }
                if (i < lines.size) i++ // skip closing fence
                blocks.add(Block.Code(code))
            }
            t.isBlank() -> i++
            t.startsWith("#") -> {
                val level = t.takeWhile { it == '#' }.length
                if (level in 1..6 && (t.length == level || t[level] == ' ')) {
                    blocks.add(Block.Header(level, t.removePrefix("#".repeat(level)).trim()))
                } else {
                    blocks.add(Block.Paragraph(t))
                }
                i++
            }
            isHr(t) -> {
                blocks.add(Block.Hr)
                i++
            }
            imageLine.matches(t) -> {
                val m = imageLine.find(t)!!
                blocks.add(Block.Image(m.groupValues[1], m.groupValues[2].trim()))
                i++
            }
            t.startsWith(">") -> {
                val quote = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quote.add(lines[i].removePrefix(">").trim())
                    i++
                }
                blocks.add(Block.Blockquote(quote.joinToString(" ")))
            }
            t.startsWith("- ") || t.startsWith("* ") || t.startsWith("+ ") -> {
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val tt = lines[i].trim()
                    if (tt.startsWith("- ") || tt.startsWith("* ") || tt.startsWith("+ ")) {
                        items.add(tt.drop(2).trim())
                        i++
                    } else if (tt.isBlank()) break
                    else break
                }
                blocks.add(Block.BulletList(items))
            }
            t.matches(Regex("^\\d+[.)]\\s+.*")) -> {
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val tt = lines[i].trim()
                    val m = Regex("^\\d+[.)]\\s+(.*)").find(tt)
                    if (m != null) {
                        items.add(m.groupValues[1].trim())
                        i++
                    } else if (tt.isBlank()) break
                    else break
                }
                blocks.add(Block.OrderedList(items))
            }
            isTableStart(t, lines.getOrNull(i + 1)) -> {
                val header = t.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                i += 2 // separator line
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trim().contains("|")) {
                    rows.add(lines[i].split("|").map { it.trim() }.filter { it.isNotEmpty() })
                    i++
                }
                blocks.add(Block.Table(header, rows))
            }
            else -> {
                val para = mutableListOf<String>()
                while (i < lines.size) {
                    val tt = lines[i].trim()
                    if (tt.isEmpty()) break
                    if (tt.startsWith("```") || tt.startsWith("#") || isHr(tt)) break
                    para.add(tt)
                    i++
                }
                blocks.add(Block.Paragraph(para.joinToString(" ")))
            }
        }
    }
    return blocks
}

private fun isHr(t: String): Boolean =
    (t.all { it == '-' } || t.all { it == '*' } || t.all { it == '_' }) && t.length >= 3

private fun isTableStart(line: String, next: String?): Boolean {
    if (!line.contains("|")) return false
    val nextT = next?.trim() ?: return false
    return nextT.contains('-') && nextT.all { it == '-' || it == ':' || it == '|' || it == ' ' }
}