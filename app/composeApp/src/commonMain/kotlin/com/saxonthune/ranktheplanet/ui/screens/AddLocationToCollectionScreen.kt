package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import com.saxonthune.ranktheplanet.nav.Screen
import com.saxonthune.ranktheplanet.ui.MockScreen
import com.saxonthune.ranktheplanet.ui.NavAction

@Composable
fun AddLocationToCollectionScreen(onNavigate: (Screen) -> Unit) {
    MockScreen(
        title = Screen.AddLocationToCollection.title,
        actions = listOf(
            NavAction("Pick a Collection", Screen.ReviewForm),
            NavAction("New Collection", Screen.SchemaBuilder),
            NavAction("Cancel", Screen.MapOverview),
        ),
        onNavigate = onNavigate,
    )
}
