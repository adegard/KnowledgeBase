package com.knowledgebase.app.util

/**
 * Selection-aware Markdown editing helpers.
 *
 * All helpers operate on a full text plus a selection (start/end offsets in
 * characters). They return the new text together with the new selection so the
 * editor can place the cursor sensibly after the operation.
 */
object MarkdownFormatUtil {

    data class EditResult(
        val text: String,
        val selectionStart: Int,
        val selectionEnd: Int,
    )

    /** Returns the count of case-insensitive occurrences of [query] in [text]. */
    fun countMatches(text: String, query: String): Int {
        if (query.isEmpty() || text.isEmpty()) return 0
        var count = 0
        var idx = 0
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        while (true) {
            idx = lowerText.indexOf(lowerQuery, idx)
            if (idx < 0) break
            count++
            idx += query.length
        }
        return count
    }

    /** Finds the next occurrence of [query] at or after [from]. */
    fun findNext(text: String, query: String, from: Int): IntRange? {
        if (query.isEmpty()) return null
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        val idx = lowerText.indexOf(lowerQuery, from.coerceAtLeast(0))
        return if (idx >= 0) idx until (idx + query.length) else null
    }

    /** Finds the previous occurrence of [query] at or before [from]. */
    fun findPrevious(text: String, query: String, from: Int): IntRange? {
        if (query.isEmpty()) return null
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        val idx = lowerText.lastIndexOf(lowerQuery, (from - 1).coerceAtLeast(0))
        return if (idx >= 0) idx until (idx + query.length) else null
    }

    /** Replaces the first occurrence starting at [from]; returns null if none. */
    fun replaceNext(text: String, query: String, replacement: String, from: Int): EditResult? {
        val range = findNext(text, query, from) ?: return null
        val newText = text.substring(0, range.first) + replacement + text.substring(range.last + 1)
        val cursor = range.first + replacement.length
        return EditResult(newText, cursor, cursor)
    }

    fun replaceAll(text: String, query: String, replacement: String): String {
        if (query.isEmpty()) return text
        return text.replace(query, replacement, ignoreCase = false)
    }

    /**
     * Applies a formatting [action] to [text] around [selStart]..[selEnd] and
     * returns the new text + selection.
     */
    fun apply(action: String, text: String, selStart: Int, selEnd: Int): EditResult {
        val min = selStart.coerceIn(0, text.length)
        val max = selEnd.coerceIn(0, text.length).coerceAtLeast(min)
        val selected = text.substring(min, max)
        return when (action) {
            "bold", "italic", "strike", "code" -> {
                wrapBlock(action, text, min, max, selected)
            }
            "h1", "h2", "h3" -> {
                val marker = when (action) {
                    "h1" -> "# "
                    "h2" -> "## "
                    else -> "### "
                }
                prefixLines(text, min, max, marker)
            }
            "ul" -> prefixLines(text, min, max, "- ")
            "ol" -> numberLines(text, min, max)
            "checkbox" -> prefixLines(text, min, max, "- [ ] ")
            "quote" -> prefixLines(text, min, max, "> ")
            "link" -> insertTemplateOrWrap(text, min, max, selected, "]", "](https://)")
            "image" -> {
                val template = "![]()"
                if (selected.isEmpty()) insertTemplate(text, min, template)
                else {
                    EditResult(
                        text.substring(0, min) + "![$selected](https://)" + text.substring(max),
                        min + 1,
                        min + 1 + selected.length
                    )
                }
            }
            "table" -> run {
                val template = "| Header | Value |\n| --- | ---: |\n| Cell 1 | 2 |\n"
                if (selected.isEmpty()) insertTemplate(text, min, template)
                else EditResult(
                    text.substring(0, min) + template + text.substring(max),
                    min,
                    min
                )
            }
            else -> EditResult(text, min, max)
        }
    }

    private fun wrapBlock(
        action: String,
        text: String,
        min: Int,
        max: Int,
        selected: String
    ): EditResult {
        val marker = when (action) {
            "bold" -> "**"
            "italic" -> "*"
            "strike" -> "~~"
            else -> "`"
        }
        if (selected.isEmpty()) {
            return insertTemplate(text, min, "$marker$marker")
        }
        return EditResult(
            text.substring(0, min) + marker + selected + marker + text.substring(max),
            min + marker.length,
            min + marker.length + selected.length
        )
    }

    private fun prefixLines(text: String, min: Int, max: Int, prefix: String): EditResult {
        val textMax = max.coerceAtMost(text.length)
        // Expand to full lines
        var lineStart = min
        while (lineStart > 0 && text[lineStart - 1] != '\n') lineStart--
        var lineEnd = textMax
        if (lineEnd < text.length && text[lineEnd] != '\n') {
            while (lineEnd < text.length && text[lineEnd] != '\n') lineEnd++
        }
        val lines = text.substring(lineStart, lineEnd).split("\n")
        val prefixed = lines.joinToString("\n") { prefix + it }
        val newText = text.substring(0, lineStart) + prefixed + text.substring(lineEnd)
        val shift = prefix.length * lines.size
        return EditResult(newText, min + prefix.length, max + shift)
    }

    private fun numberLines(text: String, min: Int, max: Int): EditResult {
        val textMax = max.coerceAtMost(text.length)
        var lineStart = min
        while (lineStart > 0 && text[lineStart - 1] != '\n') lineStart--
        var lineEnd = textMax
        if (lineEnd < text.length && text[lineEnd] != '\n') {
            while (lineEnd < text.length && text[lineEnd] != '\n') lineEnd++
        }
        val lines = text.substring(lineStart, lineEnd).split("\n")
        val indexed = lines.mapIndexed { i, l -> "${i + 1}. $l" }
        val newText = text.substring(0, lineStart) + indexed.joinToString("\n") + text.substring(lineEnd)
        val shift = indexed.sumOf { it.length } - lines.sumOf { it.length }
        // Select the whole freshly-numbered block
        return EditResult(newText, lineStart, lineEnd + shift)
    }

    private fun insertTemplateOrWrap(
        text: String,
        min: Int,
        max: Int,
        selected: String,
        close: String,
        tail: String
    ): EditResult {
        if (selected.isEmpty()) {
            return insertTemplate(text, min, "[$close$tail")
        }
        return EditResult(
            text.substring(0, min) + "[" + selected + tail + text.substring(max),
            min + 1,
            min + 1 + selected.length
        )
    }

    private fun insertTemplate(text: String, at: Int, template: String): EditResult {
        return EditResult(
            text.substring(0, at) + template + text.substring(at),
            at + template.length / 2,
            at + template.length / 2
        )
    }
}