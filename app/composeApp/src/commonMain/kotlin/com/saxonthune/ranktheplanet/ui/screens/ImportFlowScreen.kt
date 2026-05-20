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
fun ImportFlowScreen(onFinish: () -> Unit, onCancel: () -> Unit) {
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
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Finish the import")
            }
        }
    }
}
