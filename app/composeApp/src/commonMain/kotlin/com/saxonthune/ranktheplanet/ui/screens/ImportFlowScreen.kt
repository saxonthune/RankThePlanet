package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import com.saxonthune.ranktheplanet.nav.Screen
import com.saxonthune.ranktheplanet.ui.MockScreen
import com.saxonthune.ranktheplanet.ui.NavAction

@Composable
fun ImportFlowScreen(onNavigate: (Screen) -> Unit) {
    MockScreen(
        title = Screen.ImportFlow.title,
        actions = listOf(
            NavAction("Finish the import", Screen.CollectionList),
            NavAction("Cancel", Screen.CollectionList),
        ),
        onNavigate = onNavigate,
    )
}
