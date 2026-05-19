package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import com.saxonthune.ranktheplanet.nav.Screen
import com.saxonthune.ranktheplanet.ui.MockScreen
import com.saxonthune.ranktheplanet.ui.NavAction

@Composable
fun CollectionDetailScreen(onNavigate: (Screen) -> Unit) {
    MockScreen(
        title = Screen.CollectionDetail.title,
        actions = listOf(
            NavAction("Toggle map/list projection", Screen.CollectionDetail),
            NavAction("Open an entry", Screen.CollectionEntryDetail),
            NavAction("Add an entry", Screen.LocationPicker),
            NavAction("Edit the template", Screen.SchemaBuilder),
            NavAction("Back", Screen.CollectionList),
        ),
        onNavigate = onNavigate,
    )
}
