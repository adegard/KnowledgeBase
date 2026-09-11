package com.knowledgebase.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.knowledgebase.app.ui.components.MoveDialog
import com.knowledgebase.app.ui.components.RenamingItem
import com.knowledgebase.app.ui.components.Sidebar
import com.knowledgebase.app.ui.components.TextPromptDialog
import com.knowledgebase.app.ui.screens.EditorScreen
import com.knowledgebase.app.ui.screens.FolderPickerScreen
import com.knowledgebase.app.ui.theme.KnowledgeBaseTheme
import com.knowledgebase.app.ui.viewmodel.MainViewModel
import com.knowledgebase.app.ui.viewmodel.MainUiState
import com.knowledgebase.app.ui.viewmodel.OpenTab
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KnowledgeBaseAppRoot()
        }
    }

    @Suppress("DEPRECATION")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun KnowledgeBaseAppRoot() {
        val context = LocalContext.current
        val viewModel: MainViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        KnowledgeBaseTheme(darkTheme = uiState.isDarkMode) {
            val snackbarHostState = remember { SnackbarHostState() }
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            // All-files access settings screen (opens the system Settings page)
            val openAllFilesSettings: () -> Unit = {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                Unit
            }

            // Surface any previous crash so a blank screen is never unexplained
            var crashText by remember { mutableStateOf(KnowledgeBaseApp.readCrashLog(context)) }
            if (crashText != null) {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("Previous crash details") },
                    text = {
                        Text(
                            crashText ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 20
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            KnowledgeBaseApp.clearCrashLog(context)
                            crashText = null
                        }) { Text("OK") }
                    }
                )
            }

            // Messages / toasts
            LaunchedEffect(uiState.infoMessage) {
                uiState.infoMessage?.let {
                    snackbarHostState.showSnackbar(it)
                    viewModel.clearMessages()
                }
            }
            LaunchedEffect(uiState.errorMessage) {
                uiState.errorMessage?.let {
                    snackbarHostState.showSnackbar(it)
                    viewModel.clearMessages()
                }
            }

            // Always keep a live diagnostic file the user can share
            val diagTab = uiState.openTabs.firstOrNull {
                it.notePath == uiState.activeTabPath
            }
            LaunchedEffect(
                uiState.activeTabPath, uiState.hasRoot, uiState.kbUri,
                diagTab?.content?.length, diagTab?.loadError, uiState.saveStatus
            ) {
                if (uiState.hasRoot) writeDiagFile(context, uiState, diagTab, null)
            }

            // Dialog states
            var showCreateFileDialog by remember { mutableStateOf(false) }
            var showCreateFolderDialog by remember { mutableStateOf(false) }
            var renaming by remember { mutableStateOf<RenamingItem?>(null) }
            var moving by remember { mutableStateOf<RenamingItem?>(null) }
            var deletePath by remember { mutableStateOf<String?>(null) }

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(
                                    WindowInsets.statusBars
                                        .union(WindowInsets.displayCutout)
                                        .only(WindowInsetsSides.Top)
                                )
                        ) {
                            val activeTab = uiState.openTabs.firstOrNull {
                                it.notePath == uiState.activeTabPath
                            }
                            // Drawer header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Knowledge Base",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        uiState.kbName ?: "No folder selected",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            // Quick actions (always visible at top of menu)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                DrawerActionButton("＋ File", enabled = uiState.hasRoot) {
                                    showCreateFileDialog = true
                                }
                                DrawerActionButton("Folder", enabled = uiState.hasRoot) {
                                    showCreateFolderDialog = true
                                }
                                DrawerActionButton("Rename", enabled = activeTab != null) {
                                    activeTab?.let {
                                        renaming = RenamingItem(it.notePath, it.name, isFolder = false)
                                    }
                                }
                                DrawerActionButton("Delete", enabled = activeTab != null) {
                                    activeTab?.let { deletePath = it.notePath }
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                DrawerActionButton("Storage") { openAllFilesSettings() }
                                DrawerActionButton("Switch KB") { viewModel.clearKnowledgeBase() }
                                DrawerActionButton("Theme") { viewModel.toggleDarkMode() }
                                DrawerActionButton("Refresh") { viewModel.refreshTree() }
                            }
                            Surface(
                                onClick = {
                                    writeDiagFile(context, uiState, diagTab, null)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Diagnostics saved to /Download/kb-diag.txt"
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    "Diagnose (save dump to /Download/kb-diag.txt)",
                                    modifier = Modifier.padding(12.dp, 6.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider()

                            Sidebar(
                                modifier = Modifier.weight(1f),
                                tree = uiState.tree,
                                isLoading = uiState.isTreeLoading,
                                searchQuery = uiState.searchQuery,
                                searchResults = uiState.searchResults,
                                activeTabPath = uiState.activeTabPath,
                                selectedFolderPath = uiState.selectedFolderPath,
                                onSearch = { viewModel.searchNotes(it) },
                                onOpenFile = { path, name, uri ->
                                    viewModel.openNote(path, name, uri)
                                    scope.launch { drawerState.close() }
                                },
                                onToggleFolder = {},
                                onSelectFolder = { path -> viewModel.setSelectedFolder(path) },
                                onRename = { item -> renaming = item },
                                onMove = { item -> moving = item },
                                onDelete = { path -> deletePath = path },
                                onClearSearch = { viewModel.searchNotes("") }
                            )
                        }
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // ---- Top bar (manual, no Scaffold) ----
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                WindowInsets.statusBars
                                    .union(WindowInsets.displayCutout)
                                    .only(WindowInsetsSides.Top)
                            )
                    ) {
                        val activeTab = uiState.openTabs.firstOrNull {
                            it.notePath == uiState.activeTabPath
                        }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, "Open sidebar")
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Canvas(Modifier.width(10.dp).height(10.dp)) {
                                            drawCircle(
                                                color = if (uiState.hasRoot) Color(0xFF2E9E3B) else Color(0xFFD32F2F)
                                            )
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            uiState.kbName?.let { "KB — $it" } ?: "Knowledge Base",
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.refreshTree() }) {
                                    Icon(Icons.Default.Refresh, "Refresh tree")
                                }
                            }
                            Text(
                                if (uiState.hasRoot) {
                                    val errSuffix = activeTab?.loadError?.let { " | ERR:$it" } ?: ""
                                    "${uiState.kbUri ?: "?"} | tabs:${uiState.openTabs.size} | " +
                                        "active:${uiState.activeTabPath ?: "none"} | len:${activeTab?.content?.length ?: 0}$errSuffix"
                                } else "NO KNOWLEDGE BASE SELECTED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 9.sp
                                ),
                                color = if (uiState.hasRoot) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp)
                            )
                        }
                    }

                    // ---- Main content ----
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        if (uiState.hasRoot) {
                            EditorScreen(
                                viewModel = viewModel
                            ) { scope.launch { drawerState.open() } }
                        } else {
                            FolderPickerScreen(
                                hasRoot = uiState.hasRoot,
                                kbName = uiState.kbName,
                                onPickFolder = { path ->
                                    viewModel.setKnowledgeBase(
                                        path,
                                        path.substringAfterLast('/')
                                    )
                                },
                                onUseSample = {
                                    val sample = createSampleKnowledgeBase(context)
                                    viewModel.setKnowledgeBase(sample, "Sample Knowledge Base")
                                },
                                onOpenAllFilesSettings = openAllFilesSettings
                            )
                        }
                    }

                    // Error/info snackbar (last child = bottom of screen)
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

            // Dialogs
            if (showCreateFileDialog) {
                TextPromptDialog(
                    title = "New File",
                    label = "File name (.md)",
                    confirmText = "Create",
                    onConfirm = { name ->
                        viewModel.createFile(name)
                        showCreateFileDialog = false
                    },
                    onDismiss = { showCreateFileDialog = false }
                )
            }

            if (showCreateFolderDialog) {
                TextPromptDialog(
                    title = "New Folder",
                    label = "Folder name",
                    confirmText = "Create",
                    onConfirm = { name ->
                        viewModel.createFolder(name)
                        showCreateFolderDialog = false
                    },
                    onDismiss = { showCreateFolderDialog = false }
                )
            }

            renaming?.let { item ->
                TextPromptDialog(
                    title = if (item.isFolder) "Rename Folder" else "Rename File",
                    label = "New name",
                    initialValue = item.currentName.removeSuffix(".md"),
                    confirmText = "Rename",
                    onConfirm = { newName ->
                        viewModel.renameDocument(item.path, newName)
                        renaming = null
                    },
                    onDismiss = { renaming = null }
                )
            }

            moving?.let { item ->
                MoveDialog(
                    folders = uiState.tree,
                    itemName = item.currentName,
                    onConfirm = { dest ->
                        viewModel.moveDocument(item.path, dest)
                        moving = null
                    },
                    onDismiss = { moving = null }
                )
            }

            deletePath?.let { path ->
                AlertDialog(
                    onDismissRequest = { deletePath = null },
                    title = { Text("Delete") },
                    text = { Text("Are you sure you want to delete \"$path\"?") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteDocument(path)
                            deletePath = null
                        }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { deletePath = null }) { Text("Cancel") }
                    }
                )
            }
    }
    }
    }

    @Composable
    private fun DrawerActionButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
        val bg = MaterialTheme.colorScheme.surfaceVariant
        val fg = MaterialTheme.colorScheme.onSurfaceVariant
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = MaterialTheme.shapes.small,
            color = if (enabled) bg else bg.copy(alpha = 0.4f)
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) fg else fg.copy(alpha = 0.4f)
            )
        }
    }

    private fun createSampleKnowledgeBase(context: android.content.Context): String {
        val baseDir = File(context.getExternalFilesDir(null), "SampleKB")
        if (baseDir.exists().not()) baseDir.mkdirs()
        File(baseDir, "Welcome to Knowledge Base.md").writeText(
            """
            # Welcome to Knowledge Base!

            This is your **sample knowledge base**. Create notes in Markdown and they will be
            saved instantly.

            ## Features

            - Live markdown preview with **bold**, _italic_, `` code ``, tables, lists & more
            - Math support (${'$'}LaTeX${'$'}) and Mermaid diagrams
            - Full text search across all notes
            - Multiple tabs, autosave, find & replace
            - Dark / light theme

            ## Example Mermaid diagram

            ```mermaid
            flowchart LR
                A[Write note] --> B[Preview]
                B --> C[Save automatically]
            ```

            ## Try some math

            ${'$'}${'$'}E = mc^2${'$'}${'$'}
            """.trimIndent()
        )
        File(baseDir, "Getting Started.md").writeText(
            """
            # Getting Started

            1. Use **＋ File** to create a new note
            2. Open a folder and select a markdown file
            3. Start writing Markdown on the left, watch the preview update on the right
            4. Your notes are stored as plain `.md` files

            > Tip: long-press a file in the sidebar to rename, move or delete it.
            """.trimIndent()
        )
        File(baseDir, "Tips & Shortcuts.md").writeText(
            """
            # Tips & Shortcuts

            | Action | Shortcut |
            | --- | --- |
            | Bold | Ctrl+B |
            | Italic | Ctrl+I |
            | Find & replace | Ctrl+F |
            | New tab | Ctrl+N |

            ## Lists

            - [ ] Task one
            - [x] Task two
            """.trimIndent()
        )
        val samplesDir = File(baseDir, "Examples")
        samplesDir.mkdirs()
        File(samplesDir, "Cheatsheet.md").writeText(
            """
            # Markdown Cheatsheet

            ## Headers

            Use `#` for h1 up to `###` for h3.

            ## Emphasis

            **Bold**, *italic*, ~~strikethrough~~, `inline code`

            ## Lists

            - Bullet
            1. Numbered

            ## Blockquote

            > A wise quote here

            ## Table

            | Name | Value |
            | --- | ---: |
            | A | 1 |
            | B | 2 |

            ## Math

            Inline math ${'$'}x^2${'$'} and display: ${'$'}${'$'}\int_0^1 x^2 dx${'$'}${'$'}
            """.trimIndent()
        )
        return baseDir.absolutePath
    }
}

@Suppress("DEPRECATION")
private fun writeDiagFile(
    context: android.content.Context,
    uiState: MainUiState,
    tab: OpenTab?,
    extra: String?
): String {
    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    return try {
        val sb = StringBuilder()
        sb.appendLine("time=${System.currentTimeMillis()}")
        sb.appendLine("KB:${if (uiState.hasRoot) "ON" else "OFF"} | ${uiState.kbUri ?: "no-path"}")
        sb.appendLine("name:${uiState.kbName ?: "-"} | dark:${uiState.isDarkMode}")
        sb.appendLine("tabs:${uiState.openTabs.size} | active:${tab?.notePath ?: "none"} | len:${tab?.content?.length ?: 0} | err:${tab?.loadError ?: "-"}")
        sb.appendLine("content:${tab?.content?.replace("\n", "\\n") ?: ""}")
        extra?.let { sb.appendLine(it) }
        val file = File(dir, "kb-diag.txt")
        file.writeText(sb.toString())
        "Saved: $file"
    } catch (e: Exception) {
        "Write failed: ${e.message}"
    }
}