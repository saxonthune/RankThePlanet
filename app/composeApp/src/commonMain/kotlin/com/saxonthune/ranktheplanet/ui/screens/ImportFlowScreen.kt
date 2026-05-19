package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import com.saxonthune.ranktheplanet.ui.MockScreen
import com.saxonthune.ranktheplanet.ui.NavAction

@Composable
fun ImportFlowScreen(onFinish: () -> Unit, onCancel: () -> Unit) {
    MockScreen(
        title = "Import",
        actions = listOf(
            NavAction("Finish the import", onFinish),
            NavAction("Cancel", onCancel),
        ),
    )
}
