package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import com.saxonthune.ranktheplanet.ui.RtpDrillDownScaffold
import com.saxonthune.ranktheplanet.ui.RtpEmptyState
import com.saxonthune.ranktheplanet.ui.RtpErrorState
import com.saxonthune.ranktheplanet.ui.RtpSkeletonRow
import kotlinx.coroutines.delay

@Composable
fun CollectionDetailScreen(
    collectionId: CollectionId,
    collections: CollectionRepository,
    entries: EntryRepository,
    templates: TemplateRepository,
    onAddEntry: () -> Unit,
    onEditCollection: () -> Unit,
    onBack: () -> Unit,
    onOpenEntry: (EntryId) -> Unit,
) {
    val vm = viewModel {
        CollectionDetailViewModel(collectionId, collections, entries, templates)
    }
    val state by vm.uiState.collectAsState()
    var detailsExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val showSkeletons by produceState(false, state.isLoading) {
        if (state.isLoading) {
            delay(400)
            value = true
        } else {
            value = false
        }
    }

    RtpDrillDownScaffold(
        title = state.collection?.name ?: "Collection",
        onBack = onBack,
        actions = {
            IconButton(onClick = onEditCollection) {
                Icon(Icons.Default.Edit, contentDescription = "Edit collection")
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddEntry,
                text = { Text("Add entry") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    AssistChip(
                        onClick = { sortMenuExpanded = true },
                        label = { Text("Sort: ${sortModeLabel(state.sortMode)}") },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = null,
                            )
                        },
                        trailingIcon = if (state.sortMode != SortMode.PowerRank) {
                            {
                                IconButton(onClick = { vm.toggleSortDirection() }) {
                                    Icon(
                                        imageVector = if (state.sortDirection == SortDirection.Ascending)
                                            Icons.Default.ArrowUpward
                                        else
                                            Icons.Default.ArrowDownward,
                                        contentDescription = if (state.sortDirection == SortDirection.Ascending)
                                            "Sort ascending"
                                        else
                                            "Sort descending",
                                    )
                                }
                            }
                        } else null,
                    )
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                    ) {
                        state.availableSortModes.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(sortModeLabel(mode)) },
                                onClick = {
                                    vm.setSort(mode)
                                    sortMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            when {
                state.error != null -> RtpErrorState(
                    message = state.error!!,
                    onRetry = vm::retry,
                )
                state.isLoading && state.entries.isEmpty() && showSkeletons -> Column {
                    repeat(5) { RtpSkeletonRow() }
                }
                state.entries.isEmpty() && !state.isLoading -> RtpEmptyState(
                    icon = Icons.Default.Place,
                    title = "No entries yet",
                    body = "Add places you want to remember, rate, and revisit.",
                    actionLabel = "Add your first entry",
                    onAction = onAddEntry,
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.entries, key = { it.id.value }) { row ->
                        EntryRow(row = row, onClick = { onOpenEntry(row.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private fun sortModeLabel(mode: SortMode): String = when (mode) {
    SortMode.DateAdded -> "Date Added"
    SortMode.ReviewTime -> "Review Time"
    SortMode.Score -> "Score"
    SortMode.PowerRank -> "Power Rank"
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
