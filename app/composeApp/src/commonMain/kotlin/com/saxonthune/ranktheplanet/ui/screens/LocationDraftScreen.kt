package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
private const val MOCK_COORDS = "40.7580, -73.9855"

private data class NearbyCandidate(val name: String, val detail: String, val distance: String)

private val MOCK_NEARBY = listOf(
    NearbyCandidate("Bryant Park", "Park", "0.1 mi"),
    NearbyCandidate("New York Public Library", "Library", "0.2 mi"),
    NearbyCandidate("Sardi's", "Restaurant · American", "0.3 mi"),
)

@Composable
fun LocationDraftScreen(onClose: () -> Unit, onAddToCollection: () -> Unit) {
    var adopted by remember { mutableStateOf<String?>(null) }

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
                text = "Location Draft",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) {
                Text("Close")
            }
        }

        HorizontalDivider()

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Dropped pin",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = MOCK_COORDS,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (adopted == null) {
                Button(onClick = { adopted = null }) {
                    Text("Keep coordinates only")
                }
            } else {
                OutlinedButton(onClick = { adopted = null }) {
                    Text("Keep coordinates only")
                }
            }
            Button(onClick = onAddToCollection) {
                Text("Add to a Collection")
            }
        }

        HorizontalDivider()

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
            TextButton(onClick = { /* mockup: candidates are static */ }) {
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
                            .clickable { adopted = candidate.name }
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
                        if (adopted == candidate.name) {
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
