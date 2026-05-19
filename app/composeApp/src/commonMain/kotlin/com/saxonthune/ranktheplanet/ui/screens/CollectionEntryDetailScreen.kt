package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import com.saxonthune.ranktheplanet.nav.Screen
import com.saxonthune.ranktheplanet.ui.MockScreen
import com.saxonthune.ranktheplanet.ui.NavAction

@Composable
fun CollectionEntryDetailScreen(onNavigate: (Screen) -> Unit) {
    MockScreen(
        title = Screen.CollectionEntryDetail.title,
        actions = listOf(
            NavAction("Edit the Review", Screen.ReviewForm),
            NavAction("Remove the Collection Entry", Screen.CollectionDetail),
            NavAction("Back", Screen.CollectionDetail),
        ),
        onNavigate = onNavigate,
    )
}
