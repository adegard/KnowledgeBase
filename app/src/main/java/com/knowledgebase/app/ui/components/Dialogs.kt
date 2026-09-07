package com.knowledgebase.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.knowledgebase.app.data.model.TreeNode

@Composable
fun TextPromptDialog(
    title: String,
    label: String,
    initialValue: String = "",
    confirmText: String = "OK",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConfirm(value) })
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveDialog(
    folders: List<TreeNode>,
    itemName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(folders.firstOrNull()?.path ?: "") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move \"$itemName\"") },
        text = {
            Column {
                Text(
                    "Choose destination folder",
                    style = MaterialTheme.typography.bodyMedium
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selected,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Destination folder") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("(root)") },
                            onClick = { selected = ""; expanded = false }
                        )
                        val items = mutableListOf<TreeNode.Folder>()
                        collectFolders(folders, items)
                        items.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder.path) },
                                onClick = { selected = folder.path; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text("Move") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun collectFolders(nodes: List<TreeNode>, out: MutableList<TreeNode.Folder>) {
    nodes.forEach { node ->
        if (node is TreeNode.Folder) {
            out.add(node)
            collectFolders(node.children, out)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OutlinePanel(
    headings: List<Pair<String, Int>>,
    onNavigate: (String) -> Unit
) {
    if (headings.isEmpty()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("No headings", style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        headings.forEach { (text, level) ->
            val indentation = (level - 1) * 12
            Text(
                text = text,
                style = when (level) {
                    1 -> MaterialTheme.typography.titleSmall
                    else -> MaterialTheme.typography.bodySmall
                },
                modifier = Modifier
                    .padding(start = indentation.dp, top = 2.dp, bottom = 2.dp)
                    .combinedClickable(onClick = { onNavigate(text) }),
                color = if (level == 1) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

fun extractHeadings(markdown: String): List<Pair<String, Int>> {
    val headings = mutableListOf<Pair<String, Int>>()
    markdown.lineSequence().forEach { line ->
        val trimmed = line.trimStart()
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length
            if (level in 1..3) {
                headings.add(trimmed.drop(level).trim() to level)
            }
        }
    }
    return headings
}