package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class FieldType { STARS, TEXT, ENUM, BOOLEAN, DATE, POWER_RANKING }

private data class TemplateField(
    val name: String,
    val label: String,
    val type: FieldType,
    val required: Boolean = false,
    val options: List<String> = emptyList(),
)

// Mock Review template — stands in for the Collection's authored template.
private val MOCK_TEMPLATE = listOf(
    TemplateField("stars", "Stars", FieldType.STARS, required = true),
    TemplateField("style", "Roast style", FieldType.ENUM, options = listOf("light", "medium", "dark")),
    TemplateField("visited", "Visited", FieldType.BOOLEAN),
    TemplateField("visitedOn", "Visited on", FieldType.DATE),
    TemplateField("rank", "Power ranking", FieldType.POWER_RANKING),
    TemplateField("notes", "Notes", FieldType.TEXT),
)

// Mock draft-review — pre-filled by Review.start on entry (unset fields absent).
private val MOCK_DRAFT = mapOf(
    "stars" to "4",
    "style" to "light",
    "visited" to "true",
    "visitedOn" to "2026-05-12",
)

@Composable
fun ReviewFormScreen(onSave: () -> Unit, onCancel: () -> Unit) {
    val values = remember { mutableStateMapOf<String, String>().apply { putAll(MOCK_DRAFT) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Review · Blue Bottle Mint Plaza",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }

        HorizontalDivider()

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            MOCK_TEMPLATE.forEach { field ->
                FieldRow(
                    field = field,
                    value = values[field.name],
                    onEdit = { values[field.name] = it },
                    onClear = { values.remove(field.name) },
                )
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save the Review")
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
                TextButton(onClick = onClear) {
                    Text("Clear")
                }
            }
        }

        when (field.type) {
            FieldType.STARS -> {
                val filled = value?.toIntOrNull() ?: 0
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { i ->
                        Text(
                            text = if (i <= filled) "★" else "☆",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.clickable { onEdit(i.toString()) },
                        )
                    }
                }
            }

            FieldType.ENUM -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    field.options.forEach { option ->
                        FilterChip(
                            selected = value == option,
                            onClick = { onEdit(option) },
                            label = { Text(option) },
                        )
                    }
                }
            }

            FieldType.BOOLEAN -> {
                Switch(
                    checked = value == "true",
                    onCheckedChange = { onEdit(it.toString()) },
                )
            }

            FieldType.DATE -> {
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

            FieldType.POWER_RANKING -> {
                Text(
                    text = value?.let { "Ranked #$it" } ?: "Unranked — drag into position in the Collection",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }

            FieldType.TEXT -> {
                OutlinedTextField(
                    value = value.orEmpty(),
                    onValueChange = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add a note") },
                )
            }
        }
    }
}
