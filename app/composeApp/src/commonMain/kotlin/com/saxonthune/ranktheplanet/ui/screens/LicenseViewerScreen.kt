package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.saxonthune.ranktheplanet.ui.RtpDrillDownScaffold
import org.jetbrains.compose.resources.ExperimentalResourceApi
import ranktheplanet.composeapp.generated.resources.Res

@OptIn(ExperimentalResourceApi::class)
@Composable
fun LicenseViewerScreen(onBack: () -> Unit) {
    val text by produceState<String?>(initialValue = null) {
        value = Res.readBytes("files/LICENSE.txt").decodeToString()
    }
    RtpDrillDownScaffold(
        title = "License",
        onBack = onBack,
    ) { paddingValues ->
        Text(
            text = text.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        )
    }
}
