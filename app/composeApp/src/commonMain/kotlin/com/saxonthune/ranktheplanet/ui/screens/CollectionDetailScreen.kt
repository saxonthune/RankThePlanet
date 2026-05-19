package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.nav.Screen

@Composable
fun CollectionDetailScreen(
    collectionId: CollectionId,
    collections: CollectionRepository,
    entries: EntryRepository,
    templates: TemplateRepository,
    onNavigate: (Screen) -> Unit,
    onOpenEntry: (EntryId) -> Unit,
) {
    val vm = viewModel(key = collectionId.value) {
        CollectionDetailViewModel(collectionId, collections, entries, templates)
    }
    val state by vm.uiState.collectAsState()
    var detailsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.collection?.name ?: "Collection",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onNavigate(Screen.LocationPicker) }) { Text("Add Entry") }
            TextButton(onClick = { onNavigate(Screen.SchemaBuilder) }) { Text("Edit Template") }
            TextButton(onClick = { onNavigate(Screen.CollectionList) }) { Text("Back") }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { detailsExpanded = !detailsExpanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Details",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (detailsExpanded) "▲" else "▼",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (detailsExpanded) {
            state.collection?.let { col ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Entries: ${state.entries.size}", style = MaterialTheme.typography.bodyMedium)
                    Text("Created: ${col.created}", style = MaterialTheme.typography.bodySmall)
                    Text("Last modified: ${col.lastModified}", style = MaterialTheme.typography.bodySmall)
                    Text("Template version: ${col.templateVersion}", style = MaterialTheme.typography.bodySmall)
                    Text("Color: ${col.appearance.color}", style = MaterialTheme.typography.bodySmall)
                    Text("Pin style: ${col.appearance.pinStyle}", style = MaterialTheme.typography.bodySmall)
                    Text("Visible: ${col.isVisible}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text("Near Me") },
                enabled = false,
            )
            FilterChip(
                selected = state.sortMode == SortMode.DateAdded,
                onClick = { vm.setSort(SortMode.DateAdded) },
                label = { Text("Date Added") },
            )
            FilterChip(
                selected = state.sortMode == SortMode.ReviewTime,
                onClick = { vm.setSort(SortMode.ReviewTime) },
                label = { Text("Review Time") },
            )
            if (state.scoreFieldName != null) {
                FilterChip(
                    selected = state.sortMode == SortMode.Score,
                    onClick = { vm.setSort(SortMode.Score) },
                    label = { Text("Score") },
                )
            }
        }

        if (state.entries.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No entries yet. Tap Add Entry to start.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.entries, key = { it.id.value }) { row ->
                    EntryRow(row = row, onClick = { onOpenEntry(row.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun EntryRow(row: EntryRowUi, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = row.displayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            if (row.score != null) {
                Text(
                    text = row.score.toInt().toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (row.reviewSummary.isNotEmpty()) {
            Text(
                text = row.reviewSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
