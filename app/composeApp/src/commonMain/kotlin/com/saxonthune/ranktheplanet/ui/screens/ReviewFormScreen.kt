package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

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
                verticalArrangement = Arrangement.spacedBy(12.dp),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FieldRow(
    field: TemplateField,
    value: String?,
    onEdit: (String) -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = field.label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                TextButton(onClick = onClear) { Text("Clear") }
            }
        }

        when (field.type) {
            FieldType.Score -> {
                val config = field.config as? TemplateFieldConfig.Score
                val max = config?.max?.toInt() ?: 5
                val filled = value?.toIntOrNull() ?: 0
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    (1..max).forEach { i ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics {
                                    contentDescription = "$i out of $max"
                                    selected = i == filled
                                }
                                .clickable { onEdit(i.toString()) },
                        ) {
                            Text(
                                text = if (i <= filled) "★" else "☆",
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (i <= filled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            FieldType.Enum -> {
                val config = field.config as? TemplateFieldConfig.Enum
                val options = config?.options ?: persistentListOf()
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                val focusManager = LocalFocusManager.current
                OutlinedTextField(
                    value = value.orEmpty(),
                    onValueChange = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add a note") },
                    maxLines = Int.MAX_VALUE,
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                )
            }
            FieldType.Boolean -> {
                val checked = value == "true"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(
                            value = checked,
                            role = Role.Switch,
                            onValueChange = { onEdit(it.toString()) },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(if (checked) "Yes" else "No", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = checked, onCheckedChange = null)
                }
            }
            FieldType.Date -> DateField(value = value, onEdit = onEdit)
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun DateField(value: String?, onEdit: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    Surface(
        onClick = { showPicker = true },
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = value ?: "Pick a date",
                style = MaterialTheme.typography.bodyLarge,
                color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            if (value != null) Text("Change", style = MaterialTheme.typography.labelLarge)
        }
    }
    if (showPicker) {
        val initialMillis = value
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.atStartOfDayIn(TimeZone.UTC)
            ?.toEpochMilliseconds()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC)
                            .date
                        onEdit(date.toString())
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
