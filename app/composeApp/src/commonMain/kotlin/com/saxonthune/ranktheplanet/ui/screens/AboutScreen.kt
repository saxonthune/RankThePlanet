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
import com.saxonthune.ranktheplanet.APP_VERSION
import com.saxonthune.ranktheplanet.ui.RtpDrillDownScaffold

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onViewLicense: () -> Unit,
    onViewAttributions: () -> Unit,
    onViewSource: () -> Unit,
) {
    RtpDrillDownScaffold(
        title = "About",
        onBack = onBack,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "RankThePlanet",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Version $APP_VERSION",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Copyright © Saxon Thune. Licensed under GNU AGPLv3.\nThis program is distributed WITHOUT ANY WARRANTY.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AboutRow(label = "View license", subtitle = "GNU Affero General Public License v3", onClick = onViewLicense)
            AboutRow(label = "Open-source attributions", subtitle = "Third-party library notices", onClick = onViewAttributions)
            AboutRow(label = "View source on GitHub", subtitle = "github.com/saxonthune/RankThePlanet", onClick = onViewSource)
        }
    }
}

@Composable
private fun AboutRow(label: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
