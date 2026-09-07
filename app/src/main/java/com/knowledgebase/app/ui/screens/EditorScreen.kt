package com.knowledgebase.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.knowledgebase.app.ui.components.EditorAndPreview
import com.knowledgebase.app.ui.components.EditorToolbar
import com.knowledgebase.app.ui.components.FindReplaceBar
import com.knowledgebase.app.ui.components.OutlinePanel
import com.knowledgebase.app.ui.components.TabBar
import com.knowledgebase.app.ui.components.extractHeadings
import com.knowledgebase.app.ui.viewmodel.MainViewModel
import com.knowledgebase.app.ui.viewmodel.SaveStatus
import com.knowledgebase.app.util.MarkdownFormatUtil

@Composable
fun EditorScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showEditor by remember { mutableStateOf(true) }
    var showPreview by remember { mutableStateOf(true) }
    var showOutline by remember { mutableStateOf(false) }
    var showFindBar by remember { mutableStateOf(false) }
    var splitRatio by remember { mutableStateOf(0.5f) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var findCursor by remember { mutableStateOf(0) }

    val activeTab = uiState.openTabs.firstOrNull { it.notePath == uiState.activeTabPath }
    val activeName = activeTab?.name ?: ""
    val activeContent = uiState.openTabs
        .firstOrNull { it.notePath == uiState.activeTabPath }?.content ?: ""

    // Editor state: hoisted as TextFieldValue so we can read the selection
    // and reposition the cursor after formatting / find operations.
    var editorState by remember { mutableStateOf(TextFieldValue("")) }

    var fontSize by remember { mutableStateOf(13f) }

    // Re-sync the editor whenever the active tab OR its external content
    // changes (file opened, content loaded from disk, tab switched). Typing
    // updates content without bumping the revision, so the cursor is preserved.
    LaunchedEffect(uiState.activeTabPath, activeTab?.contentRevision) {
        val tab = uiState.openTabs.firstOrNull { it.notePath == uiState.activeTabPath }
        editorState = TextFieldValue(tab?.content ?: "")
        findCursor = 0
    }

    val wordCount = activeContent.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    val matchCount = MarkdownFormatUtil.countMatches(activeContent, findText)

    // ---- Layout diagnostics (written once per settle so we get ground truth) ----
    var mainAreaH by remember { mutableStateOf(-1) }
    var totalHpx by remember { mutableStateOf(-1) }
    var chromeHpx by remember { mutableStateOf(-1) }
    var saveHpx by remember { mutableStateOf(-1) }
    var totalCons by remember { mutableStateOf("") }
    var chromeCons by remember { mutableStateOf("") }
    var editorCons by remember { mutableStateOf("") }
    var saveCons by remember { mutableStateOf("") }
    var chromeY by remember { mutableStateOf(-1) }
    var editorY by remember { mutableStateOf(-1) }
    var saveY by remember { mutableStateOf(-1) }
    var toolbarCons by remember { mutableStateOf("") }
    var tabCons by remember { mutableStateOf("") }
    var fileNameCons by remember { mutableStateOf("") }
    var findCons by remember { mutableStateOf("") }
    var errCons by remember { mutableStateOf("") }
    val density = LocalDensity.current

    fun Modifier.consLog(sink: (String) -> Unit): Modifier =
        this.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            sink("b=$constraints =>${placeable.width}x${placeable.height}")
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }

    LaunchedEffect(
        mainAreaH, totalHpx, chromeHpx, saveHpx,
        totalCons, chromeCons, editorCons, saveCons,
        chromeY, editorY, saveY,
        toolbarCons, tabCons, fileNameCons, findCons, errCons,
        activeContent.length
    ) {
        runCatching {
            val f = java.io.File(
                android.os.Environment.getExternalStorageDirectory(),
                "Download/layout.txt"
            )
            f.writeText(
                "mainArea=$mainAreaH total=$totalHpx chrome=$chromeHpx save=$saveHpx " +
                    "contentLen=${activeContent.length} " +
                    "tabs=${uiState.openTabs.size} active=${uiState.activeTabPath ?: "none"}\n" +
                    "totalCons: $totalCons\n" +
                    "chromeCons: $chromeCons\n" +
                    "editorCons: $editorCons\n" +
                    "saveCons: $saveCons\n" +
                    "chromeY=$chromeY editorY=$editorY saveY=$saveY\n" +
                    "toolbarCons: $toolbarCons\n" +
                    "tabCons: $tabCons\n" +
                    "fileNameCons: $fileNameCons\n" +
                    "findCons: $findCons\n" +
                    "errCons: $errCons\n"
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { totalHpx = it.height }
            .consLog { totalCons = it }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { chromeHpx = it.height }
                .onGloballyPositioned { chromeY = it.positionInWindow().y.toInt() }
                .consLog { chromeCons = it }
        ) {
        Box(Modifier.consLog { toolbarCons = it }) {
            EditorToolbar(
                isDarkMode = uiState.isDarkMode,
            showFindBar = showFindBar,
            isSaveActive = uiState.saveStatus != SaveStatus.Idle,
            saveLabel = saveStatusLabel(uiState.saveStatus),
            onFormat = { action ->
                val path = activeTab?.notePath ?: return@EditorToolbar
                val sel = editorState.selection
                val result = MarkdownFormatUtil.apply(action, editorState.text, sel.min, sel.max)
                if (result.text != editorState.text) {
                    viewModel.updateContent(path, result.text)
                }
                editorState = editorState.copy(
                    text = result.text,
                    selection = TextRange(
                        result.selectionStart.coerceIn(0, result.text.length),
                        result.selectionEnd.coerceIn(0, result.text.length)
                    )
                )
            },
            onToggleOutline = { showOutline = !showOutline },
            onToggleEditor = {
                showEditor = !showEditor
                if (!showEditor && !showPreview) showPreview = true
            },
            onFindBar = {
                showFindBar = !showFindBar
                findCursor = 0
            },
            onToggleTheme = { viewModel.toggleDarkMode() }
        )
        }
        Box(Modifier.consLog { tabCons = it }) {
            TabBar(
                tabs = uiState.openTabs,
                activeTabPath = uiState.activeTabPath,
                onSelectTab = {
                    viewModel.setActiveTab(it)
                    findCursor = 0
                },
                onCloseTab = { viewModel.closeTab(it) }
            )
        }

        // Filename bar merged with font-size controls and word count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp, vertical = 2.dp)
                .heightIn(max = 52.dp)
                .consLog { fileNameCons = it },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                if (activeName.isEmpty()) "No file open" else activeName,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                color = if (activeName.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = { fontSize = (fontSize - 1f).coerceAtLeast(8f) },
                modifier = Modifier.width(28.dp).height(28.dp)
            ) {
                Icon(Icons.Default.Remove, "Decrease font size", Modifier.width(14.dp).height(14.dp))
            }
            Text(
                "${fontSize.toInt()}px",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = { fontSize = (fontSize + 1f).coerceAtMost(26f) },
                modifier = Modifier.width(28.dp).height(28.dp)
            ) {
                Icon(Icons.Default.Add, "Increase font size", Modifier.width(14.dp).height(14.dp))
            }
            Text(
                "$wordCount w",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider()

        // Visible read-failure diagnostic (never silent)
        activeTab?.loadError?.let { err ->
            Box(Modifier.consLog { errCons = it }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    err,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 3
                )
            }
            }
        }

        if (showFindBar) {
            Box(
                Modifier
                    .heightIn(max = 220.dp)
                    .consLog { findCons = it })
            {
            FindReplaceBar(
                findText = findText,
                replaceText = replaceText,
                matchCount = matchCount,
                onFindChange = {
                    findText = it
                    findCursor = 0
                },
                onReplaceChange = { replaceText = it },
                onFindNext = {
                    val range = MarkdownFormatUtil.findNext(editorState.text, findText, findCursor)
                        ?: MarkdownFormatUtil.findNext(editorState.text, findText, 0)
                    if (range != null) {
                        findCursor = range.first
                        editorState = editorState.copy(
                            selection = TextRange(range.first, range.last + 1)
                        )
                    }
                },
                onFindPrev = {
                    val range = MarkdownFormatUtil.findPrevious(editorState.text, findText, findCursor)
                        ?: MarkdownFormatUtil.findPrevious(
                            editorState.text, findText, editorState.text.length
                        )
                    if (range != null) {
                        findCursor = range.first
                        editorState = editorState.copy(
                            selection = TextRange(range.first, range.last + 1)
                        )
                    }
                },
                onReplaceOne = {
                    val path = activeTab?.notePath ?: return@FindReplaceBar
                    val result = MarkdownFormatUtil.replaceNext(
                        editorState.text, findText, replaceText, findCursor
                    ) ?: MarkdownFormatUtil.replaceNext(editorState.text, findText, replaceText, 0)
                        ?: return@FindReplaceBar
                    viewModel.updateContent(path, result.text)
                    findCursor = result.selectionEnd
                    editorState = editorState.copy(
                        text = result.text,
                        selection = TextRange(result.selectionEnd.coerceIn(0, result.text.length))
                    )
                },
                onReplaceAll = {
                    val path = activeTab?.notePath ?: return@FindReplaceBar
                    val replaced = MarkdownFormatUtil.replaceAll(activeContent, findText, replaceText)
                    if (replaced != activeContent) {
                        viewModel.updateContent(path, replaced)
                        editorState = TextFieldValue(replaced, TextRange(replaced.length))
                    }
                },
                onClose = { showFindBar = false }
            )
            }
        }
        } // end chrome Column

        // Editor area: give it the remaining height explicitly (weight() is
        // unreliable inside these nesting levels, so compute from measurements).
        val editorHdp = if (totalHpx > 0 && chromeHpx > 0 && saveHpx >= 0 &&
            (totalHpx - chromeHpx - saveHpx) > 0
        ) {
            val px = (totalHpx - chromeHpx - saveHpx)
            (px / density.density).dp
        } else {
            320.dp
        }

        // Main area: editor + preview, and optional outline panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(editorHdp)
                .onSizeChanged { mainAreaH = it.height }
                .onGloballyPositioned { editorY = it.positionInWindow().y.toInt() }
                .consLog { editorCons = it }
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                EditorAndPreview(
                    editorState = editorState,
                    onEditorStateChange = { newState -> editorState = newState },
                    onContentChange = { newContent ->
                        uiState.activeTabPath?.let { viewModel.updateContent(it, newContent) }
                    },
                    showEditor = showEditor,
                    showPreview = showPreview,
                    splitRatio = splitRatio,
                    onSplitRatioChange = { splitRatio = it },
                    isDarkMode = uiState.isDarkMode,
                    fontSize = fontSize,
                    notePath = activeTab?.notePath,
                    kbRoot = uiState.kbUri
                )
            }
            if (showOutline) {
                Surface(modifier = Modifier.width(200.dp)) {
                    OutlinePanel(
                        headings = extractHeadings(activeContent),
                        onNavigate = { }
                    )
                }
            }
        }

        // Save status strip (with root path always visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .heightIn(max = 48.dp)
                .onSizeChanged { saveHpx = it.height }
                .onGloballyPositioned { saveY = it.positionInWindow().y.toInt() }
                .consLog { saveCons = it },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                uiState.kbUri ?: "no knowledge base",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(6.dp)
                    .background(
                        when (uiState.saveStatus) {
                            SaveStatus.Saving -> MaterialTheme.colorScheme.tertiary
                            SaveStatus.Saved -> MaterialTheme.colorScheme.secondary
                            SaveStatus.Error -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        CircleShape
                    )
            )
            Text(
                saveStatusLabel(uiState.saveStatus),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun saveStatusLabel(status: SaveStatus): String = when (status) {
    SaveStatus.Saving -> "Saving…"
    SaveStatus.Saved -> "Saved"
    SaveStatus.Error -> "Save error"
    SaveStatus.Idle -> "—"
}