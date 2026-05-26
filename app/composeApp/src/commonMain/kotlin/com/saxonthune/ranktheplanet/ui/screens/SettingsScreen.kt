package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saxonthune.ranktheplanet.ui.RtpDrillDownScaffold

@Composable
fun SettingsScreen(onBack: () -> Unit, onManageProviders: () -> Unit, onOpenAbout: () -> Unit, onOpenDebug: () -> Unit) {
    RtpDrillDownScaffold(
        title = "Settings",
        onBack = onBack,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsRow(
                label = "Manage providers",
                subtitle = "Add a provider or change the default",
                enabled = true,
                onClick = onManageProviders,
            )
            SettingsRow(
                label = "About",
                subtitle = "License, attributions, source",
                enabled = true,
                onClick = onOpenAbout,
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
            SettingsRow(
                label = "Debug",
                subtitle = "Developer tools",
                enabled = true,
                onClick = onOpenDebug,
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
