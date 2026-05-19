package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import com.saxonthune.ranktheplanet.ui.MockScreen
import com.saxonthune.ranktheplanet.ui.NavAction

@Composable
fun CollectionEditorScreen(onFinish: () -> Unit, onCancel: () -> Unit) {
    MockScreen(
        title = "Edit Collection",
        actions = listOf(
            NavAction("Finish editing", onFinish),
            NavAction("Cancel", onCancel),
        ),
    )
}
