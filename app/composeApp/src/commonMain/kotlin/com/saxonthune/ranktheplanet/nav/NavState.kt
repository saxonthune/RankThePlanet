package com.saxonthune.ranktheplanet.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Minimal navigation state for the UI mockup — a single current [Screen].
 * Every transition is an explicit target (the statechart's BACK targets are
 * static), so no back stack is needed yet. Unfold one when runtime history
 * is required (see doc02.02.01).
 */
class NavState {
    var current by mutableStateOf(Screen.MapOverview)
        private set

    fun go(target: Screen) {
        current = target
    }
}

@Composable
fun rememberNavState(): NavState = remember { NavState() }
