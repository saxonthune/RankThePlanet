package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.saxonthune.ranktheplanet.domain.FieldType
import com.saxonthune.ranktheplanet.domain.TemplateField
import com.saxonthune.ranktheplanet.domain.TemplateFieldConfig
import com.saxonthune.ranktheplanet.ui.RtpModalScaffold
import com.saxonthune.ranktheplanet.ui.dismissKeyboardOnTap
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ReviewFormScreen(
    state: ReviewFormUiState,
    onEdit: (String, String) -> Unit,
    onClear: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    RtpModalScaffold(
        title = state.title,
        onCancel = onCancel,
        onSave = onSave,
        saveEnabled = !state.isSaving && state.error == null && !state.isLoading,
        saveLabel = "Save",
    ) { paddingValues ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .dismissKeyboardOnTap()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                state.fields.forEach { field ->
                    FieldRow(
                        field = field,
                        value = state.draft[field.name],
                        onEdit = { v -> onEdit(field.name, v) },
                        onClear = { onClear(field.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldRow(
    field: TemplateField,
    value: String?,
    onEdit: (String) -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = field.label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            if (field.required) {
                Text(
                    text = "Required",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (value != null) {
                TextButton(onClick = onClear) { Text("Clear") }
            }
        }

        when (field.type) {
            FieldType.Score -> {
                val config = field.config as? TemplateFieldConfig.Score
                val max = config?.max?.toInt() ?: 5
                val filled = value?.toIntOrNull() ?: 0
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..max).forEach { i ->
                        Text(
                            text = if (i <= filled) "★" else "☆",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.clickable { onEdit(i.toString()) },
                        )
                    }
                }
            }
            FieldType.Enum -> {
                val config = field.config as? TemplateFieldConfig.Enum
                val options = config?.options ?: persistentListOf()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { option ->
                        FilterChip(
                            selected = value == option,
                            onClick = { onEdit(option) },
                            label = { Text(option) },
                        )
                    }
                }
            }
            FieldType.Text -> {
                val config = field.config as? TemplateFieldConfig.Text
                val focusManager = LocalFocusManager.current
                val multiline = config?.multiline == true
                OutlinedTextField(
                    value = value.orEmpty(),
                    onValueChange = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add a note") },
                    maxLines = if (multiline) Int.MAX_VALUE else 1,
                    singleLine = !multiline,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                )
            }
            FieldType.Boolean -> {
                Switch(
                    checked = value == "true",
                    onCheckedChange = { onEdit(it.toString()) },
                )
            }
            FieldType.Date -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = value ?: "Not set",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onEdit("2026-05-19") }) {
                        Text("Pick a date")
                    }
                }
            }
            FieldType.PowerRanking -> {
                Text(
                    text = value?.let { "Ranked #$it" } ?: "Unranked",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
