package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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

@Composable
fun LocationDraftSheet(
    draft: LocationDraftSheet.Open,
    mode: MapMode,
    nearbyCandidates: List<NearbyCandidateUi>,
    isResolvingNearby: Boolean,
    onDismiss: () -> Unit,
    onCancelAdd: () -> Unit,
    onAddToCollection: () -> Unit,
    onFindNearby: () -> Unit,
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
            if (isResolvingNearby) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = onFindNearby) {
                    Text(if (nearbyCandidates.isEmpty()) "Find nearby places" else "Refresh")
                }
            }
        }

        if (nearbyCandidates.isEmpty()) {
            Text(
                text = if (isResolvingNearby) {
                    "Searching nearby…"
                } else {
                    "No nearby places — the coordinates can still be kept as-is."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                nearbyCandidates.forEach { candidate ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAdoptCandidate(candidate.displayName) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = candidate.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            if (candidate.detail != null) {
                                Text(
                                    text = candidate.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (draft.adoptedCandidate == candidate.displayName) {
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
