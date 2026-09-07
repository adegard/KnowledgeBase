package com.knowledgebase.app.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import com.knowledgebase.app.data.model.Note
import com.knowledgebase.app.data.model.TreeNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Handles reading/writing markdown files and folders in the selected
 * knowledge base using plain [File] IO (with "All files access" granted).
 *
 * A root is a real directory on disk. [setRoot] also migrates legacy SAF
 * tree URIs (content://) back to their real path when possible.
 */
class FileRepository(private val context: Context) {

    private var rootFile: File? = null

    /**
     * Accepts either a plain filesystem path or a legacy SAF tree URI.
     * SAF content:// tree URIs are converted to the underlying path when the
     * volume is the primary shared storage.
     */
    fun setRoot(uriString: String?) {
        rootFile = null
        val path = uriString?.let { convertRootToPath(it) } ?: return
        rootFile = File(path).takeIf { it.isDirectory }
    }

    fun convertRootToPath(raw: String): String? {
        if (!raw.startsWith("content://")) {
            val f = File(raw)
            return if (f.isDirectory) raw else null
        }
        return try {
            val docId = DocumentsContract.getTreeDocumentId(Uri.parse(raw))
            val idx = docId.indexOf(':')
            if (idx <= 0) return null
            val volume = docId.substring(0, idx)
            val rel = docId.substring(idx + 1)
            val base = if (volume == "primary") Environment.getExternalStorageDirectory()
            else Environment.getExternalStorageDirectory()
            val candidate = File(base, rel)
            if (candidate.isDirectory) candidate.absolutePath else null
        } catch (e: Exception) {
            null
        }
    }

    /** Returns the absolute path of the active root, or null if none. */
    fun rootPath(): String? = rootFile?.absolutePath

    /** Returns the root as a content-style URI (file://) for display purposes. */
    fun getRootUri(): Uri? = rootFile?.let { Uri.fromFile(it) }

    fun hasRoot(): Boolean = rootFile != null

    suspend fun listTree(): List<TreeNode> = withContext(Dispatchers.IO) {
        val root = rootFile ?: return@withContext emptyList()
        buildTree(root, "")
    }

    private fun buildTree(dir: File, path: String): List<TreeNode> {
        val children = dir.listFiles()
            ?.sortedWith(compareBy({ it.isFile }, { it.name.lowercase() }))
            ?: emptyList()
        return children.mapNotNull { file ->
            val childPath = if (path.isEmpty()) (file.name ?: "") else "$path/${file.name}"
            if (file.isDirectory) {
                TreeNode.Folder(
                    id = file.absolutePath,
                    path = childPath,
                    name = file.name ?: "Folder",
                    children = buildTree(file, childPath)
                )
            } else if ((file.name ?: "").lowercase().endsWith(".md")) {
                TreeNode.File(
                    id = file.absolutePath,
                    uri = file.absolutePath,
                    path = childPath,
                    name = file.name ?: "File",
                    lastModified = file.lastModified()
                )
            } else {
                null
            }
        }
    }

    suspend fun readNote(path: String, uri: String? = null): Note? = withContext(Dispatchers.IO) {
        val file = resolveDocument(path, uri) ?: return@withContext null
        if (!file.isFile || !file.canRead()) return@withContext null
        val content = readFileText(file) ?: return@withContext null
        Note(
            id = file.absolutePath,
            path = path,
            name = file.name ?: path,
            content = content,
            parentFolderPath = path.substringBeforeLast('/', ""),
            lastModified = file.lastModified()
        )
    }

    suspend fun writeNote(note: Note): Boolean = withContext(Dispatchers.IO) {
        val file = resolveDocument(note.path)
            ?: File(rootFile ?: return@withContext false, note.path)
        try {
            file.parentFile?.mkdirs()
            file.writeText(note.content, Charsets.UTF_8)
            true
        } catch (e: IOException) {
            false
        }
    }

    suspend fun createFile(parentPath: String, name: String): Boolean = withContext(Dispatchers.IO) {
        val parent = resolveDir(parentPath) ?: return@withContext false
        val safe = sanitizeFileName(name, ".md")
        val file = File(parent, safe)
        if (file.exists()) return@withContext false
        try {
            file.createNewFile()
            true
        } catch (e: IOException) {
            false
        }
    }

    suspend fun createFolder(parentPath: String, name: String): Boolean = withContext(Dispatchers.IO) {
        val parent = resolveDir(parentPath) ?: return@withContext false
        val safe = sanitizeFileName(name, "")
        try {
            val dir = File(parent, safe)
            if (dir.exists()) false else dir.mkdir()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun renameDocument(path: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val file = resolveDocument(path) ?: return@withContext false
        val suffix = if (file.isDirectory) "" else ".md"
        val safe = sanitizeFileName(newName, suffix)
        if (safe == file.name) return@withContext true
        val dest = File(file.parentFile ?: return@withContext false, safe)
        if (dest.exists()) return@withContext false
        try {
            file.renameTo(dest)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun moveDocument(path: String, targetFolderPath: String): Boolean = withContext(Dispatchers.IO) {
        val file = resolveDocument(path) ?: return@withContext false
        val target = resolveDir(targetFolderPath) ?: return@withContext false
        // Prevent moving a folder into itself
        if (file.isDirectory && target.absolutePath.startsWith(file.absolutePath)) {
            return@withContext false
        }
        val dest = File(target, file.name)
        try {
            if (dest.exists()) dest.deleteRecursively()
            if (file.isDirectory) {
                copyRecursive(file, dest)
                file.deleteRecursively()
            } else {
                file.copyTo(dest, overwrite = true)
                file.delete()
            }
        } catch (e: Exception) {
            return@withContext false
        }
        true
    }

    private fun copyRecursive(source: File, dest: File) {
        if (source.isDirectory) {
            if (!dest.exists()) dest.mkdirs()
            source.listFiles()?.forEach { child ->
                copyRecursive(child, File(dest, child.name))
            }
        } else {
            if (dest.parentFile != null && !dest.parentFile.exists()) dest.parentFile.mkdirs()
            source.copyTo(dest, overwrite = true)
        }
    }

    suspend fun deleteDocument(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = resolveDocument(path) ?: return@withContext false
        try {
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun searchNotes(query: String): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val root = rootFile ?: return@withContext emptyList()
        val results = mutableListOf<Pair<String, String>>()
        val q = query.lowercase()
        searchRecursive(root, "", q, results)
        results
    }

    private fun searchRecursive(
        dir: File,
        path: String,
        query: String,
        results: MutableList<Pair<String, String>>
    ) {
        dir.listFiles()?.forEach { file ->
            val filePath = if (path.isEmpty()) (file.name ?: "") else "$path/${file.name}"
            if (file.isDirectory) {
                searchRecursive(file, filePath, query, results)
            } else if (file.name.lowercase().endsWith(".md")) {
                try {
                    val content = readFileText(file)
                    if (content != null && content.lowercase().contains(query)) {
                        results.add(filePath to file.absolutePath)
                    }
                } catch (_: IOException) {
                }
            }
        }
    }

    suspend fun readTextDocument(path: String): String? = withContext(Dispatchers.IO) {
        val file = resolveDocument(path) ?: return@withContext null
        readFileText(file)
    }

    private fun resolveDocument(path: String, uri: String? = null): File? {
        val root = rootFile ?: return null
        val candidates = ArrayList<File>(3)
        uri?.takeIf { it.startsWith("/") }?.let { candidates.add(File(it)) }
        candidates.add(File(root, path))
        candidates.add(File(root.absolutePath, path))
        for (c in candidates) {
            if (c.isFile) return c
        }
        return null
    }

    private fun resolveDir(path: String): File? {
        val root = rootFile ?: return null
        if (path.isEmpty()) return root
        val dir = File(root, path)
        return if (dir.isDirectory) dir else null
    }

    private fun readFileText(file: File): String? {
        return try {
            file.inputStream().bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: IOException) {
            null
        }
    }

    private fun sanitizeFileName(name: String, defaultSuffix: String): String {
        var clean = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        if (clean.isEmpty()) clean = "untitled"
        if (defaultSuffix.isNotEmpty() && !clean.lowercase().endsWith(".md")) {
            clean += defaultSuffix
        }
        if (clean.length > 80) clean = clean.substring(0, 80)
        return clean
    }
}