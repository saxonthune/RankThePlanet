package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
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
    onManualNameChange: (String) -> Unit,
) {
    val useCoordinatesSelected = draft.adoptedCandidate == null
    val effectiveTitle = draft.adoptedCandidate
        ?: draft.manualName.takeIf { it.isNotBlank() }
        ?: draft.displayName

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        TopBar(
            mode = mode,
            onDismiss = onDismiss,
            onCancelAdd = onCancelAdd,
            onAddToCollection = onAddToCollection,
        )
        Preview(
            title = effectiveTitle,
            lat = draft.lat,
            lng = draft.lng,
        )
        Spacer(Modifier.height(16.dp))
        CandidatesHeader(
            isResolvingNearby = isResolvingNearby,
            hasResults = nearbyCandidates.isNotEmpty(),
            onFindNearby = onFindNearby,
        )
        UseCoordinatesRow(
            selected = useCoordinatesSelected,
            manualName = draft.manualName,
            onSelect = onKeepCoordinates,
            onManualNameChange = onManualNameChange,
        )
        nearbyCandidates.forEach { candidate ->
            CandidateRow(
                candidate = candidate,
                selected = draft.adoptedCandidate == candidate.displayName,
                onSelect = { onAdoptCandidate(candidate.displayName) },
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TopBar(
    mode: MapMode,
    onDismiss: () -> Unit,
    onCancelAdd: () -> Unit,
    onAddToCollection: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
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
        Spacer(Modifier.weight(1f))
        FilledTonalButton(onClick = onAddToCollection) {
            Text("Add to Collection")
        }
    }
}

@Composable
private fun Preview(
    title: String?,
    lat: Double,
    lng: Double,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = "(untitled location)",
                style = MaterialTheme.typography.headlineSmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatLatLng(lat, lng),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CandidatesHeader(
    isResolvingNearby: Boolean,
    hasResults: Boolean,
    onFindNearby: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Nearby places",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (isResolvingNearby) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            TextButton(onClick = onFindNearby) {
                Text(if (hasResults) "Refresh" else "Find nearby")
            }
        }
    }
}

@Composable
private fun UseCoordinatesRow(
    selected: Boolean,
    manualName: String,
    onSelect: () -> Unit,
    onManualNameChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onSelect)
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Use coordinates",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (selected) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = manualName,
                onValueChange = onManualNameChange,
                singleLine = true,
                placeholder = { Text("Name this place (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp),
            )
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: NearbyCandidateUi,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = candidate.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (candidate.detail != null) {
                Text(
                    text = candidate.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
