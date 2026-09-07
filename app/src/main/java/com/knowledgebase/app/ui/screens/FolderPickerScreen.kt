package com.knowledgebase.app.ui.screens

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import java.io.File

@Composable
fun FolderPickerScreen(
    hasRoot: Boolean,
    kbName: String?,
    onPickFolder: (String) -> Unit,
    onUseSample: () -> Unit,
    onOpenAllFilesSettings: () -> Unit
) {
    val context = LocalContext.current
    var accessGranted by remember { mutableStateOf(hasAllFilesAccess(context)) }

    LifecycleResumeEffect(Unit) {
        accessGranted = hasAllFilesAccess(context)
        onPauseOrDispose { }
    }

    if (!accessGranted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Knowledge Base",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "This app needs \"All files access\" so it can open and save your Markdown notes in any folder.\n\n" +
                    "In the next screen, enable it for this app, then come back.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onOpenAllFilesSettings) {
                Text("Grant Storage Access")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onUseSample) {
                Text("I'll use the Sample Knowledge Base")
            }
        }
        return
    }

    val startDir = remember {
        val ext = Environment.getExternalStorageDirectory()
        if (ext.isDirectory) ext else context.filesDir
    }
    var currentDir by remember { mutableStateOf(startDir) }
    val folders = remember(currentDir) {
        (currentDir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.sortedBy { it.name.lowercase() } ?: emptyList()).toList()
    }
    val canRead = folders.isNotEmpty() ||
        (currentDir.listFiles()?.any { it.isFile } ?: false)

    Column(modifier = Modifier.fillMaxSize()) {
        // Path + actions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                currentDir.absolutePath,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onPickFolder(currentDir.absolutePath) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Use this folder")
                }
                currentDir.parentFile?.let {
                    OutlinedButton(onClick = { currentDir = it }) {
                        Icon(Icons.Default.ArrowUpward, null, Modifier.size(16.dp))
                        Text("Up")
                    }
                }
            }
            if (hasRoot) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onUseSample,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue with: ${kbName ?: "current knowledge base"}")
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap a folder to open it. All .md files inside will become your notes.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider()

        if (!canRead) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "This folder can't be read right now.\nTap Up or use \"Grant Storage Access\".",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(folders, key = { it.absolutePath }) { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentDir = folder }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            folder.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun hasAllFilesAccess(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true
    }