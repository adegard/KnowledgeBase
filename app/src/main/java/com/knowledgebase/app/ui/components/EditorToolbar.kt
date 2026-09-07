package com.knowledgebase.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class ToolbarButton(
    val id: String,
    val icon: ImageVector,
    val label: String
)

@Composable
fun EditorToolbar(
    isDarkMode: Boolean,
    showFindBar: Boolean,
    isSaveActive: Boolean,
    saveLabel: String,
    onFormat: (String) -> Unit,
    onToggleOutline: () -> Unit,
    onToggleEditor: () -> Unit,
    onFindBar: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val formatButtons = listOf(
        ToolbarButton("bold", Icons.Default.FormatBold, "Bold"),
        ToolbarButton("italic", Icons.Default.FormatItalic, "Italic"),
        ToolbarButton("strike", Icons.Default.FormatStrikethrough, "Strikethrough"),
        ToolbarButton("code", Icons.Default.Code, "Inline code"),
        ToolbarButton("h1", Icons.Default.Title, "Heading 1"),
        ToolbarButton("h2", Icons.Default.ViewList, "Heading 2"),
        ToolbarButton("h3", Icons.Default.TextFields, "Heading 3"),
        ToolbarButton("ul", Icons.Default.FormatListBulleted, "Bullet list"),
        ToolbarButton("ol", Icons.Default.FormatListNumbered, "Numbered list"),
        ToolbarButton("checkbox", Icons.Default.CheckBox, "Task checkbox"),
        ToolbarButton("quote", Icons.Default.FormatQuote, "Blockquote"),
        ToolbarButton("table", Icons.Default.GridOn, "Insert table"),
        ToolbarButton("link", Icons.Default.Link, "Insert link"),
        ToolbarButton("image", Icons.Default.Image, "Insert image")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(onClick = onToggleEditor) {
                Icon(
                    Icons.Default.Edit,
                    "Toggle editor",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleOutline) {
                Icon(
                    Icons.Default.Title,
                    "Toggle outline",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            VerticalDivider(Modifier.padding(vertical = 8.dp))
            formatButtons.forEachIndexed { index, btn ->
                IconButton(onClick = { onFormat(btn.id) }) {
                    Icon(
                        btn.icon,
                        btn.label,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (btn.id == "h3" || btn.id == "link") {
                    VerticalDivider(Modifier.padding(vertical = 8.dp))
                }
            }
            VerticalDivider(Modifier.padding(vertical = 8.dp))
            IconButton(onClick = onFindBar) {
                Icon(
                    Icons.Default.FindInPage,
                    "Find & replace",
                    tint = if (showFindBar) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onToggleTheme) {
                Icon(
                    if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    "Toggle dark mode",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider()
    }
}