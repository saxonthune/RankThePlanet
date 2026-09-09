package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.saxonthune.ranktheplanet.ui.RtpDrillDownScaffold
import org.jetbrains.compose.resources.ExperimentalResourceApi
import ranktheplanet.composeapp.generated.resources.Res

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AttributionsScreen(onBack: () -> Unit) {
    val json by produceState<String?>(initialValue = null) {
        value = Res.readBytes("files/aboutlibraries.json").decodeToString()
    }
    RtpDrillDownScaffold(title = "Open-source", onBack = onBack) { padding ->
        json?.let { j ->
            LibrariesContainer(
                aboutLibsJson = j,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}
