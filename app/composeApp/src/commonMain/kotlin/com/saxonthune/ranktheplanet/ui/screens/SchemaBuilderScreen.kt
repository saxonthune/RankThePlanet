package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import com.saxonthune.ranktheplanet.ui.MockScreen
import com.saxonthune.ranktheplanet.ui.NavAction

@Composable
fun SchemaBuilderScreen(onFinish: () -> Unit, onCancel: () -> Unit) {
    MockScreen(
        title = "Review Template",
        actions = listOf(
            NavAction("Finish the template", onFinish),
            NavAction("Cancel", onCancel),
        ),
    )
}
