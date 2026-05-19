package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.runtime.Composable
import com.saxonthune.ranktheplanet.nav.Screen
import com.saxonthune.ranktheplanet.ui.MockScreen
import com.saxonthune.ranktheplanet.ui.NavAction

@Composable
fun SettingsScreen(onNavigate: (Screen) -> Unit) {
    MockScreen(
        title = Screen.Settings.title,
        actions = listOf(
            NavAction("Back", Screen.MapOverview),
        ),
        onNavigate = onNavigate,
    )
}
