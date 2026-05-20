package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.EntryId

@Composable
internal fun EntryDrawerSheet(
    entry: EntrySummaryUi,
    onOpenFullDetail: (EntryId) -> Unit,
    onViewCollection: (CollectionId) -> Unit,
    onEditReview: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(entry.locationName, style = MaterialTheme.typography.titleMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(entry.collectionColor, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(entry.collectionName)
        }
        if (entry.visited) {
            Text(
                "Visited",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AssistChip(
                onClick = {},
                label = { Text("Not yet visited") },
                leadingIcon = {
                    Icon(Icons.Default.Info, contentDescription = null)
                },
            )
        }

        Spacer(Modifier.height(16.dp))

        TextButton(
            onClick = { onViewCollection(entry.collectionId) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("View the Collection")
        }
        TextButton(
            onClick = { onEditReview() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Edit the Review")
        }
        Button(
            onClick = { onOpenFullDetail(entry.entryId) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open full detail")
        }
        Spacer(Modifier.height(8.dp))
    }
}
