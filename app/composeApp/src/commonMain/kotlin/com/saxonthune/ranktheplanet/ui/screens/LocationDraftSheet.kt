package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saxonthune.ranktheplanet.nav.MapMode

private data class NearbyCandidate(val name: String, val detail: String, val distance: String)

private val MOCK_NEARBY = listOf(
    NearbyCandidate("Bryant Park", "Park", "0.1 mi"),
    NearbyCandidate("New York Public Library", "Library", "0.2 mi"),
    NearbyCandidate("Sardi's", "Restaurant · American", "0.3 mi"),
)

@Composable
fun LocationDraftSheet(
    draft: LocationDraftSheet.Open,
    mode: MapMode,
    onDismiss: () -> Unit,
    onCancelAdd: () -> Unit,
    onAddToCollection: () -> Unit,
    onAdoptCandidate: (String) -> Unit,
    onKeepCoordinates: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
            }
            if (mode is MapMode.AddingToCollection) {
                TextButton(onClick = onCancelAdd) {
                    Text("Cancel adding")
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (draft.displayName != null) {
                Text(
                    text = draft.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = formatLatLng(draft.lat, draft.lng),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (draft.adoptedCandidate == null) {
                Button(
                    onClick = onKeepCoordinates,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Keep coordinates only")
                }
            } else {
                OutlinedButton(
                    onClick = onKeepCoordinates,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Keep coordinates only")
                }
            }
            Button(
                onClick = onAddToCollection,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add to a Collection")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Nearby places",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { /* stub: provider integration is out of scope */ }) {
                Text("Find nearby places")
            }
        }

        if (MOCK_NEARBY.isEmpty()) {
            Text(
                text = "No nearby places — the coordinates can still be kept as-is.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                MOCK_NEARBY.forEach { candidate ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAdoptCandidate(candidate.name) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = candidate.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "${candidate.detail} · ${candidate.distance}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (draft.adoptedCandidate == candidate.name) {
                            Text(
                                text = "Adopted",
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
}
