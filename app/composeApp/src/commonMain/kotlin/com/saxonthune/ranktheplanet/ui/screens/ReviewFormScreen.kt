package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import com.saxonthune.ranktheplanet.nav.Screen
import com.saxonthune.ranktheplanet.ui.MockScreen
import com.saxonthune.ranktheplanet.ui.NavAction

@Composable
fun ReviewFormScreen(onNavigate: (Screen) -> Unit) {
    MockScreen(
        title = Screen.ReviewForm.title,
        actions = listOf(
            NavAction("Save the Review", Screen.CollectionEntryDetail),
            NavAction("Cancel", Screen.CollectionDetail),
        ),
        onNavigate = onNavigate,
    )
}
