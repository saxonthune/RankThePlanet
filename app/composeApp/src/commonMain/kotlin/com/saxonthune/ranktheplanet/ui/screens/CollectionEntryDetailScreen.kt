package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.ui.RtpDrillDownScaffold
import com.saxonthune.ranktheplanet.ui.RtpErrorState

@Composable
fun CollectionEntryDetailScreen(
    entryId: EntryId,
    entries: EntryRepository,
    templates: TemplateRepository,
    onEditReview: (EntryId) -> Unit,
    onRemoveEntry: () -> Unit,
    onBack: () -> Unit,
    onViewCollection: (CollectionId) -> Unit,
) {
    val vm = viewModel {
        CollectionEntryDetailViewModel(entryId, entries, templates)
    }
    val state by vm.uiState.collectAsState()

    if (state.error != null) {
        RtpErrorState(message = state.error!!, onRetry = vm::retry)
        return
    }

    if (state.entry == null && !state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Entry not found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    RtpDrillDownScaffold(
        title = state.entry?.location?.displayName ?: "Entry",
        onBack = onBack,
        actions = {
            IconButton(onClick = { onEditReview(entryId) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit review")
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // location section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Location",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { state.entry?.collectionId?.let(onViewCollection) }) {
                        Text("View collection")
                    }
                }
                state.entry?.location?.let { loc ->
                    Text(loc.displayName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${loc.coordinates.lat}, ${loc.coordinates.lng}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val meta = loc.cachedMetadata
                    if (meta != null) {
                        Text(meta, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(
                            "No cached metadata",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            HorizontalDivider()

            // review section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Review", style = MaterialTheme.typography.titleMedium)
                if (!state.reviewed) {
                    Text(
                        "Not reviewed yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.fields.forEach { field ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = field.label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = if (field.isSet) field.value else "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (field.isSet) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = onRemoveEntry) { Text("Remove entry") }
            }
        }
    }
}
