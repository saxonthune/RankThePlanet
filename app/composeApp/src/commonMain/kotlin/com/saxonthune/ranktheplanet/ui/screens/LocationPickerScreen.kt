package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import com.saxonthune.ranktheplanet.nav.Screen
import com.saxonthune.ranktheplanet.ui.MockScreen
import com.saxonthune.ranktheplanet.ui.NavAction

@Composable
fun LocationPickerScreen(onNavigate: (Screen) -> Unit) {
    MockScreen(
        title = Screen.LocationPicker.title,
        actions = listOf(
            NavAction("Pick a Location", Screen.ReviewForm),
            NavAction("Cancel", Screen.CollectionDetail),
        ),
        onNavigate = onNavigate,
    )
}
