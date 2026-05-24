package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.saxonthune.ranktheplanet.domain.EntryId

@Composable
internal fun LocationDetailPeek(
    peek: PinSheet.Peek,
    onPickEntry: (EntryId) -> Unit,
    onAddEntry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DebugSheetLabel("LocationDetailPeek")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp, max = 220.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(peek.locationName, style = MaterialTheme.typography.titleMedium)
                Text(
                    formatLatLng(peek.lat, peek.lng),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(peek.entries) { entry ->
                        ListItem(
                            headlineContent = { Text(entry.collectionName) },
                            leadingContent = {
                                Box(
                                    Modifier
                                        .size(12.dp)
                                        .background(entry.collectionColor, CircleShape)
                                )
                            },
                            supportingContent = {
                                if (entry.reviewed) {
                                    Text("Reviewed", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("Not yet reviewed") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Info, contentDescription = null)
                                        },
                                    )
                                }
                            },
                            modifier = Modifier.clickable { onPickEntry(entry.entryId) },
                        )
                    }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAddEntry)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(
                        text = "Add another Entry at this Location",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun DebugSheetLabel(name: String) {
    Text(
        text = "debug: $name",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.error,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}
