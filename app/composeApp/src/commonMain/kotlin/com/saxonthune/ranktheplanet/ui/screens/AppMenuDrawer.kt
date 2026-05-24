package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.saxonthune.ranktheplanet.domain.CollectionId

@Composable
internal fun AppMenuDrawer(
    state: MapOverviewUiState,
    onToggleCollection: (CollectionId) -> Unit,
    onOpenSettings: () -> Unit,
) {
    ModalDrawerSheet {
        Text(
            text = "Menu",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )

        Text(
            text = "Filter",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        var filterQuery by remember { mutableStateOf("") }
        val focusManager = LocalFocusManager.current

        OutlinedTextField(
            value = filterQuery,
            onValueChange = { filterQuery = it },
            placeholder = { Text("Filter…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(8.dp))

        val filtered = state.collectionRows.filter {
            it.name.contains(filterQuery, ignoreCase = true)
        }
        LazyColumn {
            items(filtered, key = { it.id.value }) { row ->
                ListItem(
                    headlineContent = { Text(row.name) },
                    leadingContent = {
                        Box(
                            Modifier
                                .size(12.dp)
                                .background(row.color, CircleShape)
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = row.shown,
                            onCheckedChange = { onToggleCollection(row.id) },
                        )
                    },
                )
                HorizontalDivider()
            }
        }

        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Settings") },
            leadingContent = {
                Icon(Icons.Default.Settings, contentDescription = null)
            },
            modifier = Modifier.clickable { onOpenSettings() },
        )
    }
}
