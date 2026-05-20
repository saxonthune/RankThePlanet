package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saxonthune.ranktheplanet.ui.RtpModalScaffold

@Composable
fun CollectionEditorScreen(onFinish: () -> Unit, onCancel: () -> Unit) {
    RtpModalScaffold(
        title = "New collection",
        onCancel = onCancel,
        onSave = onFinish,
        saveLabel = "Save",
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Collection editor coming soon.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
