package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import com.saxonthune.ranktheplanet.ui.MockScreen
import com.saxonthune.ranktheplanet.ui.NavAction

@Composable
fun LocationPickerScreen(onPickLocation: () -> Unit, onCancel: () -> Unit) {
    MockScreen(
        title = "Pick a Location",
        actions = listOf(
            NavAction("Pick a Location", onPickLocation),
            NavAction("Cancel", onCancel),
        ),
    )
}
