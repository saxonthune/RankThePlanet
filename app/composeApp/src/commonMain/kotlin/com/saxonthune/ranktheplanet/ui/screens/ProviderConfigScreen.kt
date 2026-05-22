package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.saxonthune.ranktheplanet.ui.RtpDrillDownScaffold

@Composable
fun ProviderConfigScreen(
    viewModel: ProviderConfigViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    RtpDrillDownScaffold(title = uiState.providerName, onBack = onBack) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // status region
            Text(
                text = uiState.statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // setup region
            when (uiState.mode) {
                ProviderMode.Google -> {
                    OutlinedTextField(
                        value = uiState.keyDraft,
                        onValueChange = viewModel::onKeyDraftChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API key") },
                        placeholder = { Text("AIza...") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                    )
                    Text(
                        text = "Get a key in Google Cloud Console — enable Places API (New).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = viewModel::onSaveKey,
                        enabled = uiState.keyDraft.isNotBlank() && !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Save key")
                    }
                }
                ProviderMode.Osm -> {
                    Text(
                        text = "Endpoints: photon.komoot.io, nominatim.openstreetmap.org",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Attribution: © OpenStreetMap contributors",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ProviderMode.Fake -> {
                    Text(
                        text = "Dev provider. No setup required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // actions region
            Button(
                onClick = viewModel::onSetAsDefault,
                enabled = uiState.isConfigured && !uiState.isDefault,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Set as default")
            }
        }
    }
}
