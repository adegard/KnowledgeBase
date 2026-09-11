package com.knowledgebase.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.knowledgebase.app.data.model.TreeNode

@Composable
fun Sidebar(
    modifier: Modifier = Modifier,
    tree: List<TreeNode>,
    isLoading: Boolean,
    searchQuery: String,
    searchResults: List<Pair<String, String>>,
    activeTabPath: String?,
    selectedFolderPath: String?,
    onSearch: (String) -> Unit,
    onOpenFile: (String, String, String) -> Unit,
    onToggleFolder: (String) -> Unit,
    onSelectFolder: (String) -> Unit,
    onRename: (RenamingItem) -> Unit,
    onMove: (RenamingItem) -> Unit,
    onDelete: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    var query by remember { mutableStateOf(searchQuery) }
    var inSearchMode by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        query = searchQuery
        if (searchQuery.isBlank()) inSearchMode = false
    }

    // Track expanded folders locally per sidebar instance
    val expandedPaths = remember { mutableStateMapOf<String, Boolean>() }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    inSearchMode = it.isNotBlank()
                    onSearch(it)
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search notes…") },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = ""; inSearchMode = false; onSearch("") }) {
                            Text("✕")
                        }
                    }
                },
                textStyle = MaterialTheme.typography.bodySmall,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query) })
            )
        }

        HorizontalDivider()

        if (inSearchMode) {
            SearchResultsList(
                results = searchResults,
                onOpenFile = { path, name, uri ->
                    onOpenFile(path, name, uri)
                    inSearchMode = false
                }
            )
        } else {
            TreeList(
                tree = tree,
                isLoading = isLoading,
                expandedPaths = expandedPaths,
                activePath = activeTabPath,
                selectedFolderPath = selectedFolderPath,
                onOpenFile = onOpenFile,
                onToggleFolder = { path ->
                    expandedPaths[path] = !(expandedPaths[path] ?: true)
                    onToggleFolder(path)
                },
                onSelectFolder = onSelectFolder,
                onRename = onRename,
                onMove = onMove,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<Pair<String, String>>,
    onOpenFile: (String, String, String) -> Unit
) {
    Text(
        "${results.size} result(s)",
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (results.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No results found", style = MaterialTheme.typography.bodySmall)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(results, key = { it.first }) { (path, uri) ->
                val name = path.substringAfterLast('/')
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenFile(path, name, uri) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Article,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(name, style = MaterialTheme.typography.bodySmall)
                        Text(
                            path,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TreeList(
    tree: List<TreeNode>,
    isLoading: Boolean,
    expandedPaths: MutableMap<String, Boolean>,
    activePath: String?,
    selectedFolderPath: String?,
    onOpenFile: (String, String, String) -> Unit,
    onToggleFolder: (String) -> Unit,
    onSelectFolder: (String) -> Unit,
    onRename: (RenamingItem) -> Unit,
    onMove: (RenamingItem) -> Unit,
    onDelete: (String) -> Unit
) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    } else if (tree.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Folder is empty\n(or storage access not granted)\n\nUse  ☰ → Storage  to grant access,\nthen Refresh.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            tree.forEach { node ->
                TreeNodeRow(
                    node = node,
                    depth = 0,
                    expandedPaths = expandedPaths,
                    activePath = activePath,
                    selectedFolderPath = selectedFolderPath,
                    onOpenFile = onOpenFile,
                    onToggleFolder = onToggleFolder,
                    onSelectFolder = onSelectFolder,
                    onRename = onRename,
                    onMove = onMove,
                    onDelete = onDelete
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TreeNodeRow(
    node: TreeNode,
    depth: Int,
    expandedPaths: MutableMap<String, Boolean>,
    activePath: String?,
    selectedFolderPath: String?,
    onOpenFile: (String, String, String) -> Unit,
    onToggleFolder: (String) -> Unit,
    onSelectFolder: (String) -> Unit,
    onRename: (RenamingItem) -> Unit,
    onMove: (RenamingItem) -> Unit,
    onDelete: (String) -> Unit
) {
    when (node) {
        is TreeNode.Folder -> {
            val isExpanded = expandedPaths[node.path] ?: true
            var menuVisible by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            onToggleFolder(node.path)
                            onSelectFolder(node.path)
                        },
                        onLongClick = { menuVisible = true }
                    )
                    .padding(start = (8 + depth * 16).dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = if (node.path == selectedFolderPath) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    node.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = if (node.path == selectedFolderPath) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }

            Box {
                DropdownMenu(expanded = menuVisible, onDismissRequest = { menuVisible = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            menuVisible = false
                            onRename(RenamingItem(node.path, node.name, isFolder = true))
                        },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null, Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuVisible = false
                            onDelete(node.path)
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null, Modifier.size(18.dp)) }
                    )
                }
            }

            if (isExpanded) {
                node.children.forEach { child ->
                    TreeNodeRow(
                        node = child,
                        depth = depth + 1,
                        expandedPaths = expandedPaths,
                        activePath = activePath,
                        selectedFolderPath = selectedFolderPath,
                        onOpenFile = onOpenFile,
                        onToggleFolder = onToggleFolder,
                        onSelectFolder = onSelectFolder,
                        onRename = onRename,
                        onMove = onMove,
                        onDelete = onDelete
                    )
                }
            }
        }

        is TreeNode.File -> {
            var menuVisible by remember { mutableStateOf(false) }
            val isActive = node.path == activePath
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onOpenFile(node.path, node.name, node.uri) },
                        onLongClick = { menuVisible = true }
                    )
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                    .padding(start = (30 + depth * 16).dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Article,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    node.name.removeSuffix(".md"),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            Box {
                DropdownMenu(expanded = menuVisible, onDismissRequest = { menuVisible = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            menuVisible = false
                            onRename(RenamingItem(node.path, node.name, isFolder = false))
                        },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null, Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Move") },
                        onClick = {
                            menuVisible = false
                            onMove(RenamingItem(node.path, node.name, isFolder = false))
                        },
                        leadingIcon = { Icon(Icons.Default.ArrowForwardIos, null, Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuVisible = false
                            onDelete(node.path)
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null, Modifier.size(18.dp)) }
                    )
                }
            }
        }
    }
}

data class RenamingItem(
    val path: String,
    val currentName: String,
    val isFolder: Boolean
)