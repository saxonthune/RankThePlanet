package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import com.saxonthune.ranktheplanet.nav.Screen
import com.saxonthune.ranktheplanet.ui.MockScreen
import com.saxonthune.ranktheplanet.ui.NavAction

@Composable
fun MapOverviewScreen(onNavigate: (Screen) -> Unit) {
    MockScreen(
        title = Screen.MapOverview.title,
        actions = listOf(
            NavAction("Inspect a pin", Screen.CollectionEntryDetail),
            NavAction("Browse Collections", Screen.CollectionList),
            NavAction("Drop a pin here", Screen.AddLocationToCollection),
            NavAction("Open Settings", Screen.Settings),
        ),
        onNavigate = onNavigate,
    )
}
