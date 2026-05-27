package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

private enum class SheetMode { Browse, Selection }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CollectionListSheet(
    collections: CollectionRepository,
    entries: EntryRepository,
    preselection: Set<CollectionId>,
    onDismiss: () -> Unit,
    onTapCollection: (CollectionId) -> Unit,
    onNewCollection: () -> Unit,
    onImport: () -> Unit,
    onApplyFilter: (Set<CollectionId>) -> Unit,
) {
    val vm = viewModel { CollectionListViewModel(collections, entries) }
    val state by vm.uiState.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by remember { mutableStateOf(if (preselection.isNotEmpty()) SheetMode.Selection else SheetMode.Browse) }
    var selectedIds by remember { mutableStateOf(preselection) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (mode) {
                    SheetMode.Browse -> {
                        Text(
                            text = "Collections",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onNewCollection) { Text("New Collection") }
                        TextButton(onClick = onImport) { Text("Import") }
                    }
                    SheetMode.Selection -> {
                        Text(
                            text = "${selectedIds.size} selected",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            mode = SheetMode.Browse
                            selectedIds = emptySet()
                        }) { Text("Cancel") }
                        TextButton(
                            onClick = { onApplyFilter(selectedIds) },
                            enabled = selectedIds.isNotEmpty(),
                        ) { Text("See on map") }
                    }
                }
            }
            HorizontalDivider()
            LazyColumn {
                items(state.collections, key = { it.id.value }) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    when (mode) {
                                        SheetMode.Browse -> onTapCollection(row.id)
                                        SheetMode.Selection -> {
                                            selectedIds = if (row.id in selectedIds) selectedIds - row.id else selectedIds + row.id
                                        }
                                    }
                                },
                                onLongClick = if (mode == SheetMode.Browse) ({
                                    mode = SheetMode.Selection
                                    selectedIds = setOf(row.id)
                                }) else null,
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (mode == SheetMode.Selection) {
                            Checkbox(
                                checked = row.id in selectedIds,
                                onCheckedChange = null,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(row.color),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = row.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "${row.entryCount} ${if (row.entryCount == 1) "entry" else "entries"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
