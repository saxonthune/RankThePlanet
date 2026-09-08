package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saxonthune.ranktheplanet.ui.RtpDrillDownScaffold

@Composable
fun ImportFlowScreen(
    state: ImportFlowUiState,
    onPick: () -> Unit,
    onCancel: () -> Unit,
) {
    RtpDrillDownScaffold(
        title = "Import",
        onBack = onCancel,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Pick a KML or GeoJSON file to import.")

            when (state.phase) {
                ImportFlowUiState.Phase.Importing -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator()
                            Text(
                                if (state.totalEntries > 0) {
                                    "Importing ${state.completedEntries} of ${state.totalEntries}…"
                                } else "Reading file…"
                            )
                            if (state.totalEntries > 0) {
                                LinearProgressIndicator(
                                    progress = { state.completedEntries.toFloat() / state.totalEntries },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = onPick,
                        enabled = state.phase == ImportFlowUiState.Phase.Idle,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Pick a file")
                    }
                    if (state.error != null) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
