package com.knowledgebase.app.data.model

/**
 * A tree node representing either a file or a folder in the KB.
 */
sealed class TreeNode {
    abstract val path: String
    abstract val name: String

    data class File(
        val id: String,
        val uri: String,
        override val path: String,
        override val name: String,
        val lastModified: Long
    ) : TreeNode()

    data class Folder(
        val id: String,
        override val path: String,
        override val name: String,
        val children: List<TreeNode> = emptyList(),
        val isExpanded: Boolean = true
    ) : TreeNode()
}
