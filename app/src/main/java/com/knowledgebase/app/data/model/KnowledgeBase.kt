package com.knowledgebase.app.data.model

/**
 * Represents the currently selected knowledge base folder.
 * Uses SAF (Storage Access Framework) tree URI for persistent access.
 */
data class KnowledgeBase(
    val uri: String,
    val displayName: String,
    val rootPath: String = ""
)
