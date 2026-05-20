package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.ui.RtpDrillDownScaffold
import com.saxonthune.ranktheplanet.ui.RtpEmptyState
import com.saxonthune.ranktheplanet.ui.RtpErrorState
import com.saxonthune.ranktheplanet.ui.RtpSkeletonRow
import kotlinx.coroutines.delay

@Composable
fun CollectionListScreen(
    collections: CollectionRepository,
    entries: EntryRepository,
    onNewCollection: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
    onOpenCollection: (CollectionId) -> Unit,
) {
    val vm = viewModel { CollectionListViewModel(collections, entries) }
    val state by vm.uiState.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }

    val showSkeletons by produceState(false, state.isLoading) {
        if (state.isLoading) {
            delay(400)
            value = true
        } else {
            value = false
        }
    }

    RtpDrillDownScaffold(
        title = "Collections",
        onBack = onBack,
        actions = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Import") },
                        onClick = {
                            menuExpanded = false
                            onImport()
                        },
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewCollection,
                text = { Text("New collection") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                state.error != null -> RtpErrorState(
                    message = state.error!!,
                    onRetry = vm::retry,
                )
                state.isLoading && state.collections.isEmpty() && showSkeletons -> Column {
                    repeat(5) { RtpSkeletonRow() }
                }
                state.collections.isEmpty() && !state.isLoading -> RtpEmptyState(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "No collections yet",
                    body = "Create a list of places you want to remember, rate, and revisit.",
                    actionLabel = "Create your first collection",
                    onAction = onNewCollection,
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.collections, key = { it.id.value }) { row ->
                        CollectionRow(row = row, onClick = { onOpenCollection(row.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionRow(row: CollectionRowUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(row.color)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "${row.entryCount} ${if (row.entryCount == 1) "entry" else "entries"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
