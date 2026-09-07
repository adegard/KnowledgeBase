package com.knowledgebase.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knowledgebase.app.KnowledgeBaseApp
import com.knowledgebase.app.data.model.Note
import com.knowledgebase.app.data.model.TreeNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Serializable representation of an open tab, used to persist tabs across restarts.
 */
data class OpenTabPersistence(
    val notePath: String,
    val name: String,
    val uri: String? = null
)

data class OpenTab(
    val notePath: String,
    val name: String,
    val content: String = "",
    val isDirty: Boolean = false,
    val contentRevision: Int = 0,
    val uri: String? = null,
    val loadError: String? = null
)

data class MainUiState(
    val kbUri: String? = null,
    val kbName: String? = null,
    val hasRoot: Boolean = false,
    val tree: List<TreeNode> = emptyList(),
    val isTreeLoading: Boolean = false,
    val openTabs: List<OpenTab> = emptyList(),
    val activeTabPath: String? = null,
    val isDarkMode: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Pair<String, String>> = emptyList(),
    val isSearching: Boolean = false,
    val selectedFolderPath: String? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val saveStatus: SaveStatus = SaveStatus.Idle
)

enum class SaveStatus { Idle, Saving, Saved, Error }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as KnowledgeBaseApp
    private val repo = app.fileRepository
    private val prefs = app.preferences

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var saveJob: Job? = null
    private var revisionCounter = 0
    private fun nextRevision(): Int = ++revisionCounter

    init {
        val uri = prefs.getKbUri()
        val name = prefs.getKbName()
        val dark = prefs.isDarkMode()
        var effectiveUri = uri
        if (uri != null) {
            repo.setRoot(uri)
            // Migrate legacy SAF tree URIs to the real folder path
            repo.rootPath()?.let { path ->
                effectiveUri = path
                if (path != uri) {
                    prefs.setKnowledgeBase(path, name ?: path.substringAfterLast('/'))
                }
            }
        }
        _uiState.update {
            it.copy(
                kbUri = effectiveUri,
                kbName = name,
                hasRoot = repo.hasRoot(),
                isDarkMode = dark
            )
        }
        if (repo.hasRoot()) {
            refreshTree()
        }
        restoreTabs()
    }

    private fun restoreTabs() {
        viewModelScope.launch {
            val saved = prefs.getOpenTabs()
            val openTabs = saved.map { OpenTab(it.notePath, it.name, uri = it.uri) }
            val lastNote = prefs.getLastOpenNote()
                ?.takeIf { path -> saved.any { it.notePath == path } }
                ?: saved.firstOrNull()?.notePath
            _uiState.update {
                it.copy(openTabs = openTabs, activeTabPath = lastNote)
            }
            // Eagerly load contents so the editor can render restored notes instantly
            openTabs.forEach { tab ->
                val note = repo.readNote(tab.notePath, tab.uri)
                if (note != null) {
                    _uiState.update { state ->
                        state.copy(
                            openTabs = state.openTabs.map {
                                if (it.notePath == tab.notePath) it.copy(
                                    content = note.content,
                                    contentRevision = nextRevision()
                                ) else it
                            }
                        )
                    }
                }
            }
        }
        persistTabs()
    }

    private fun loadContentInto(path: String, uri: String? = null) {
        viewModelScope.launch {
            val tab = _uiState.value.openTabs.firstOrNull { it.notePath == path } ?: return@launch
            if (tab.content.isNotEmpty()) return@launch
            val note = repo.readNote(path, uri)
            _uiState.update { state ->
                state.copy(
                    openTabs = state.openTabs.map {
                        if (it.notePath == path) it.copy(
                            content = note?.content ?: "",
                            contentRevision = nextRevision(),
                            loadError = if (note == null) "Could not read \"${tab.name}\"" else null
                        ) else it
                    }
                )
            }
        }
    }

    private fun persistTabs() {
        viewModelScope.launch {
            val tabs = _uiState.value.openTabs
            prefs.saveOpenTabs(
                tabs.map { OpenTabPersistence(it.notePath, it.name, it.uri) }
            )
            prefs.setLastOpenNote(_uiState.value.activeTabPath)
        }
    }

    fun refreshTree() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTreeLoading = true) }
            val tree = repo.listTree()
            _uiState.update { it.copy(tree = tree, isTreeLoading = false) }
        }
    }

    fun openNote(path: String, name: String, uri: String? = null) {
        viewModelScope.launch {
            val existing = _uiState.value.openTabs.firstOrNull { it.notePath == path }
            if (existing != null) {
                setActiveTab(path)
                return@launch
            }
            // Read by exact document URI when available (robust against
            // name-based re-resolution failures for spaces/special chars).
            val note = repo.readNote(path, uri)
            val content = note?.content ?: ""
            val error = if (note == null) "Could not read \"$name\"" else null
            if (note == null && content.isEmpty()) {
                showError(error ?: "")
            }
            val tab = OpenTab(
                notePath = path,
                name = name,
                content = content,
                isDirty = false,
                contentRevision = nextRevision(),
                uri = uri ?: note?.id,
                loadError = error
            )
            _uiState.update { state ->
                state.copy(
                    openTabs = state.openTabs + tab,
                    activeTabPath = path
                )
            }
            prefs.setLastOpenNote(path)
            persistTabs()
        }
    }

    fun setActiveTab(path: String) {
        _uiState.update { it.copy(activeTabPath = path) }
        val tab = _uiState.value.openTabs.firstOrNull { it.notePath == path }
        if (tab != null && tab.content.isEmpty()) {
            loadContentInto(path, tab.uri)
        }
        prefs.setLastOpenNote(path)
        persistTabs()
    }

    fun updateContent(path: String, content: String) {
        _uiState.update { state ->
            val tabs = state.openTabs.map {
                if (it.notePath == path) it.copy(
                    content = content,
                    isDirty = true,
                    loadError = null
                ) else it
            }
            state.copy(openTabs = tabs)
        }
        scheduleSave(path, content)
    }

    private fun scheduleSave(path: String, content: String) {
        _uiState.update { it.copy(saveStatus = SaveStatus.Saving) }
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1200)
            val success = repo.writeNote(
                Note(
                    id = path,
                    path = path,
                    name = path.substringAfterLast('/'),
                    content = content,
                    parentFolderPath = path.substringBeforeLast('/', ""),
                    lastModified = System.currentTimeMillis()
                )
            )
            if (success) {
                _uiState.update { state ->
                    state.copy(
                        saveStatus = SaveStatus.Saved,
                        openTabs = state.openTabs.map {
                            if (it.notePath == path) it.copy(isDirty = false) else it
                        }
                    )
                }
            } else {
                _uiState.update { it.copy(saveStatus = SaveStatus.Error) }
            }
        }
    }

    fun closeTab(path: String) {
        val tabs = _uiState.value.openTabs.filter { it.notePath != path }
        val wasActive = _uiState.value.activeTabPath == path
        val newActive = if (wasActive) {
            val lastNote = prefs.getLastOpenNote()
            if (lastNote != null && lastNote != path && tabs.any { it.notePath == lastNote }) lastNote
            else tabs.lastOrNull()?.notePath
        } else {
            _uiState.value.activeTabPath
        }
        _uiState.update {
            it.copy(openTabs = tabs, activeTabPath = newActive)
        }
        prefs.setLastOpenNote(newActive)
        persistTabs()
    }

    fun toggleDarkMode() {
        val newValue = !_uiState.value.isDarkMode
        prefs.setDarkMode(newValue)
        _uiState.update { it.copy(isDarkMode = newValue) }
    }

    fun setSelectedFolder(path: String) {
        _uiState.update { it.copy(selectedFolderPath = path) }
    }

    fun createFile(name: String) {
        val parent = _uiState.value.selectedFolderPath ?: ""
        viewModelScope.launch {
            val ok = repo.createFile(parent, name)
            if (ok) {
                refreshTree()
                showInfo("File \"$name\" created")
            } else {
                showError("Could not create file")
            }
        }
    }

    fun createFolder(name: String) {
        val parent = _uiState.value.selectedFolderPath ?: ""
        viewModelScope.launch {
            val ok = repo.createFolder(parent, name)
            if (ok) {
                refreshTree()
                showInfo("Folder \"$name\" created")
            } else {
                showError("Could not create folder")
            }
        }
    }

    fun renameDocument(path: String, newName: String) {
        viewModelScope.launch {
            val ok = repo.renameDocument(path, newName)
            if (ok) {
                refreshTree()
                showInfo("Renamed to \"$newName\"")
            } else {
                showError("Could not rename")
            }
        }
    }

    fun moveDocument(path: String, targetFolderPath: String) {
        viewModelScope.launch {
            val ok = repo.moveDocument(path, targetFolderPath)
            if (ok) {
                refreshTree()
                showInfo("Moved successfully")
            } else {
                showError("Could not move")
            }
        }
    }

    fun deleteDocument(path: String) {
        viewModelScope.launch {
            val ok = repo.deleteDocument(path)
            if (ok) {
                closeTab(path)
                refreshTree()
                showInfo("Deleted")
            } else {
                showError("Could not delete")
            }
        }
    }

    fun searchNotes(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false) }
            return
        }
        _uiState.update { it.copy(searchQuery = query, isSearching = true) }
        viewModelScope.launch {
            val results = repo.searchNotes(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun setKnowledgeBase(uri: String, name: String) {
        if (repo.convertRootToPath(uri) == null) {
            showError("Folder is not accessible. Grant storage access first.")
            return
        }
        repo.setRoot(uri)
        val path = repo.rootPath()
        if (path == null) {
            showError("Could not use this folder")
            return
        }
        val resolvedName = name.ifBlank { path.substringAfterLast('/') }
        prefs.setKnowledgeBase(path, resolvedName)
        _uiState.update {
            it.copy(kbUri = path, kbName = resolvedName, hasRoot = true)
        }
        refreshTree()
    }

    fun clearKnowledgeBase() {
        prefs.clearKnowledgeBase()
        _uiState.update {
            it.copy(
                kbUri = null,
                kbName = null,
                hasRoot = false,
                tree = emptyList(),
                openTabs = emptyList(),
                activeTabPath = null
            )
        }
    }

    fun showInfo(message: String) {
        _uiState.update { it.copy(infoMessage = message, errorMessage = null) }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message, infoMessage = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(infoMessage = null, errorMessage = null) }
    }

    fun getOpenNoteContent(path: String): String {
        return _uiState.value.openTabs.firstOrNull { it.notePath == path }?.content ?: ""
    }
}