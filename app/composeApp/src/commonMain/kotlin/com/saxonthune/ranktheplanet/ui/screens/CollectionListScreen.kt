package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import com.saxonthune.ranktheplanet.nav.Screen
import com.saxonthune.ranktheplanet.ui.MockScreen
import com.saxonthune.ranktheplanet.ui.NavAction

@Composable
fun CollectionListScreen(onNavigate: (Screen) -> Unit) {
    MockScreen(
        title = Screen.CollectionList.title,
        actions = listOf(
            NavAction("Open a Collection", Screen.CollectionDetail),
            NavAction("New Collection", Screen.SchemaBuilder),
            NavAction("Import a list", Screen.ImportFlow),
            NavAction("Back", Screen.MapOverview),
        ),
        onNavigate = onNavigate,
    )
}
