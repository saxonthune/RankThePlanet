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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.ui.RtpDrillDownScaffold
import com.saxonthune.ranktheplanet.ui.RtpErrorState
import com.saxonthune.ranktheplanet.ui.theme.parseAppearanceColor

@Composable
fun CollectionEntryDetailScreen(
    entryId: EntryId,
    entries: EntryRepository,
    collections: CollectionRepository,
    templates: TemplateRepository,
    onEditReview: (EntryId) -> Unit,
    onRemoveEntry: () -> Unit,
    onBack: () -> Unit,
    onViewCollection: (CollectionId) -> Unit,
    onTapLocation: () -> Unit,
) {
    val vm = viewModel {
        CollectionEntryDetailViewModel(entryId, entries, collections, templates)
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

    var deleteArmed by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    RtpDrillDownScaffold(
        title = state.entry?.location?.displayName ?: "Entry",
        onBack = onBack,
        actions = {
            if (deleteArmed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Delete Entry?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    IconButton(onClick = onRemoveEntry) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Confirm delete entry",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            } else {
                IconButton(onClick = { deleteArmed = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete entry")
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // collection pill
            state.collection?.let { col ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewCollection(col.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(parseAppearanceColor(col.appearance.color)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(col.name, style = MaterialTheme.typography.bodyLarge)
                }
            }

            HorizontalDivider()

            // location section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTapLocation() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Location", style = MaterialTheme.typography.titleMedium)
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
                    OutlinedButton(
                        onClick = {
                            val url = "https://www.google.com/maps/search/?api=1&query=" +
                                "${loc.coordinates.lat},${loc.coordinates.lng}"
                            uriHandler.openUri(url)
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Open in maps")
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Review",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onEditReview(entryId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit review")
                    }
                }
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    }
                }
            }
        }
    }
}
