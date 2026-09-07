package com.knowledgebase.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun resolveBaseDir(kbRoot: String?, notePath: String?): String? {
    if (notePath == null) return null
    if (notePath.startsWith("/")) return notePath.substringBeforeLast('/', "")
    if (kbRoot.isNullOrBlank()) return null
    val rel = notePath.substringBeforeLast('/', "")
    return if (rel.isEmpty()) kbRoot else "$kbRoot/$rel"
}

@Composable
fun FindReplaceBar(
    findText: String,
    replaceText: String,
    matchCount: Int,
    onFindChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onFindNext: () -> Unit,
    onFindPrev: () -> Unit,
    onReplaceOne: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        TextField(
            value = findText,
            onValueChange = onFindChange,
            placeholder = { Text("Find") },
            modifier = Modifier.weight(1f).heightIn(min = 32.dp),
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true
        )
        Text(
            "$matchCount",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onFindPrev) {
            Icon(Icons.Default.ArrowUpward, "Previous", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onFindNext) {
            Icon(Icons.Default.ArrowDownward, "Next", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextField(
            value = replaceText,
            onValueChange = onReplaceChange,
            placeholder = { Text("Replace") },
            modifier = Modifier.weight(1f).heightIn(min = 32.dp),
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true
        )
        TextButton(onClick = onReplaceOne) {
            Text("One", style = MaterialTheme.typography.labelSmall)
        }
        TextButton(onClick = onReplaceAll) {
            Text("All", style = MaterialTheme.typography.labelSmall)
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider()
}

@Composable
fun EditorAndPreview(
    editorState: TextFieldValue,
    onEditorStateChange: (TextFieldValue) -> Unit,
    onContentChange: (String) -> Unit,
    showEditor: Boolean,
    showPreview: Boolean,
    splitRatio: Float,
    onSplitRatioChange: (Float) -> Unit,
    isDarkMode: Boolean,
    fontSize: Float,
    notePath: String? = null,
    kbRoot: String? = null,
    modifier: Modifier = Modifier
) {
    val contentForPreview = editorState.text
    var editorWidth by remember { mutableFloatStateOf(splitRatio) }
    var totalWidth by remember { mutableIntStateOf(1) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { totalWidth = it.width.coerceAtLeast(1) }
    ) {
        if (showEditor) {
            Box(
                modifier = Modifier
                    .weight(if (showPreview) editorWidth else 1f)
                    .fillMaxHeight()
            ) {
                TextField(
                    value = editorState,
                    onValueChange = { newValue ->
                        onEditorStateChange(newValue)
                        if (newValue.text != editorState.text) {
                            onContentChange(newValue.text)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    placeholder = { Text("Start writing Markdown…") },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize.sp
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        if (showEditor && showPreview) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                if (totalWidth > 0) {
                                    val newWidth =
                                        ((editorWidth * totalWidth + dragAmount) / totalWidth)
                                            .coerceIn(0.15f, 0.85f)
                                    editorWidth = newWidth
                                    onSplitRatioChange(newWidth)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight(0.6f)
                        .background(MaterialTheme.colorScheme.outline)
                )
            }
        }

        if (showPreview) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isDarkMode) Color(0xFF1E1E24) else Color(0xFFFFFFFF)
                    )
            ) {
                MarkdownPreview(
                    content = contentForPreview,
                    fontSize = fontSize,
                    modifier = Modifier.padding(start = 8.dp),
                    baseDir = resolveBaseDir(kbRoot, notePath)
                )
            }
        }
    }
}