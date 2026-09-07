package com.knowledgebase.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.knowledgebase.app.ui.viewmodel.OpenTab

@Composable
fun TabBar(
    tabs: List<OpenTab>,
    activeTabPath: String?,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .horizontalScroll(rememberScrollState())
                .height(36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isActive = tab.notePath == activeTabPath
                Row(
                    modifier = Modifier
                        .clickable { onSelectTab(tab.notePath) }
                        .background(
                            if (isActive) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(start = 12.dp, end = 4.dp)
                        .height(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tab.name.removeSuffix(".md"),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = when {
                            isActive -> MaterialTheme.colorScheme.primary
                            tab.isDirty -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.widthIn(max = 100.dp)
                    )
                    if (tab.isDirty) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                        )
                    }
                    IconButton(
                        onClick = { onCloseTab(tab.notePath) },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
        }
        HorizontalDivider()
    }
}