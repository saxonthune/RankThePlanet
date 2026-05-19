package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import com.saxonthune.ranktheplanet.ui.MockScreen
import com.saxonthune.ranktheplanet.ui.NavAction

@Composable
fun AddLocationToCollectionScreen(
    onPickCollection: () -> Unit,
    onNewCollection: () -> Unit,
    onCancel: () -> Unit,
) {
    MockScreen(
        title = "Add to Collection",
        actions = listOf(
            NavAction("Pick a Collection", onPickCollection),
            NavAction("New Collection", onNewCollection),
            NavAction("Cancel", onCancel),
        ),
    )
}
