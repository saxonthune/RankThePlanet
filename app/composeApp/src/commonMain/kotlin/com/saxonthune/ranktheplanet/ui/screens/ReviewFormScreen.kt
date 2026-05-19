package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import com.saxonthune.ranktheplanet.ui.MockScreen
import com.saxonthune.ranktheplanet.ui.NavAction

@Composable
fun ReviewFormScreen(onSave: () -> Unit, onCancel: () -> Unit) {
    MockScreen(
        title = "Review",
        actions = listOf(
            NavAction("Save the Review", onSave),
            NavAction("Cancel", onCancel),
        ),
    )
}
