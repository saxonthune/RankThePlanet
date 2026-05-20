package com.saxonthune.ranktheplanet.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RtpDrillDownScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { actions() },
            )
        },
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        content = content,
    )
}

/** Modal variant: Cancel-left + Save-right, no back arrow. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RtpModalScaffold(
    title: String,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean = true,
    saveLabel: String = "Save",
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    androidx.compose.material3.TextButton(onClick = onCancel) { Text("Cancel") }
                },
                actions = {
                    androidx.compose.material3.TextButton(
                        onClick = onSave,
                        enabled = saveEnabled,
                    ) { Text(saveLabel) }
                },
            )
        },
        content = content,
    )
}
