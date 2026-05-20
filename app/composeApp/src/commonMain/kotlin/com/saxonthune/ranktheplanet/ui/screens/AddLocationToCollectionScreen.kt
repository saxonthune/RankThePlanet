package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saxonthune.ranktheplanet.ui.RtpDrillDownScaffold

@Composable
fun AddLocationToCollectionScreen(
    addToCollectionId: String?,
    onPickCollection: () -> Unit,
    onNewCollection: () -> Unit,
    onCancel: () -> Unit,
) {
    RtpDrillDownScaffold(
        title = "Add Location",
        onBack = onCancel,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onPickCollection,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Pick a Collection")
            }
            Button(
                onClick = onNewCollection,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("New Collection")
            }
        }
    }
}
