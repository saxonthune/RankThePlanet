package com.saxonthune.ranktheplanet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.saxonthune.ranktheplanet.nav.Screen

/** A labeled navigation affordance: tapping it moves the user to [target]. */
data class NavAction(val label: String, val target: Screen)

/**
 * Shared mockup scaffold — a screen [title] over a column of buttons, one per
 * [actions] entry. Each button reads "<action> -> <target screen>".
 */
@Composable
fun MockScreen(
    title: String,
    actions: List<NavAction>,
    onNavigate: (Screen) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        actions.forEach { action ->
            Button(
                onClick = { onNavigate(action.target) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("${action.label}  ->  ${action.target.title}")
            }
        }
    }
}
