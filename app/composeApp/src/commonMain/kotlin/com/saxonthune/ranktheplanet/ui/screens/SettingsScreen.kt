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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saxonthune.ranktheplanet.nav.Screen

@Composable
fun SettingsScreen(onNavigate: (Screen) -> Unit) {
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
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onNavigate(Screen.MapOverview) }) {
                Text("Back")
            }
        }

        HorizontalDivider()

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsRow(
                label = "Manage providers",
                subtitle = "Add a provider or change the default",
                enabled = false,
            )
            SettingsRow(
                label = "Sync target",
                subtitle = "Coming soon",
                enabled = false,
            )
            SettingsRow(
                label = "BYOK keys",
                subtitle = "Coming soon",
                enabled = false,
            )
        }
    }
}

@Composable
private fun SettingsRow(label: String, subtitle: String, enabled: Boolean, onClick: () -> Unit = {}) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp)
        .let { if (enabled) it.clickable(onClick = onClick) else it }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
