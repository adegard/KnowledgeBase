package com.knowledgebase.app.data.model

/**
 * A note (markdown file) within the knowledge base.
 * [path] is the file path relative to the KB root folder, e.g. "docs/api/notes.md"
 */
data class Note(
    val id: String,
    val path: String,
    val name: String,
    val content: String,
    val parentFolderPath: String,
    val lastModified: Long,
    val isDirty: Boolean = false
)
