package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.saxonthune.ranktheplanet.domain.CollectionId
import kotlinx.collections.immutable.ImmutableList

@Composable
fun AddLocationToCollectionSheet(
    draft: LocationDraftSheet.Open,
    collections: ImmutableList<CollectionPickRowUi>,
    preSelectedCollectionId: CollectionId?,
    onBack: () -> Unit,
    onNewCollection: () -> Unit,
    onPickCollection: (CollectionId) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        DebugSheetLabel("AddLocationToCollectionSheet")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = draft.adoptedCandidate ?: draft.displayName ?: "Dropped pin",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = formatLatLng(draft.lat, draft.lng),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Pick a Collection",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNewCollection)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(
                text = "New Collection",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        HorizontalDivider()

        if (collections.isEmpty()) {
            Text(
                text = "No Collections yet — tap New Collection above to start one. The candidate stays here while you create it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        } else {
            collections.forEach { col ->
                val isPreSelected = preSelectedCollectionId == col.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickCollection(col.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(col.color),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = col.name,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "${col.entryCount} entr${if (col.entryCount == 1) "y" else "ies"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isPreSelected) {
                        Text(
                            text = "Pre-selected",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
