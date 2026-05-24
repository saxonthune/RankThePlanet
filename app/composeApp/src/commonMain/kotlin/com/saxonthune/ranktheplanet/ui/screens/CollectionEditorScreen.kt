package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saxonthune.ranktheplanet.domain.Appearance
import com.saxonthune.ranktheplanet.domain.AppearancePalette
import com.saxonthune.ranktheplanet.domain.BuiltInTemplates
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.FieldType
import com.saxonthune.ranktheplanet.domain.ReviewTemplate
import com.saxonthune.ranktheplanet.domain.TemplateFieldConfig
import com.saxonthune.ranktheplanet.ui.RtpModalScaffold
import com.saxonthune.ranktheplanet.ui.dismissKeyboardOnTap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
fun CollectionEditorScreen(
    viewModel: CollectionEditorViewModel,
    onSaved: (CollectionId) -> Unit,
    onCancel: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CollectionEditorEvent.Saved -> onSaved(event.id)
            }
        }
    }

    val title = when (uiState.mode) {
        EditorMode.Create -> "New collection"
        is EditorMode.Edit -> uiState.name.ifBlank { "Edit collection" }
    }
    val saveLabel = when (uiState.mode) {
        EditorMode.Create -> "Create"
        is EditorMode.Edit -> "Done"
    }

    RtpModalScaffold(
        title = title,
        onCancel = onCancel,
        onSave = { viewModel.save() },
        saveEnabled = uiState.canSave,
        saveLabel = saveLabel,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .dismissKeyboardOnTap()
                .verticalScroll(rememberScrollState()),
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Loading…", style = MaterialTheme.typography.bodyMedium)
                }
                return@Column
            }

            uiState.error?.let { err ->
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            MetadataSection(
                name = uiState.name,
                description = uiState.description,
                appearance = uiState.appearance,
                onNameChange = viewModel::onNameChange,
                onDescriptionChange = viewModel::onDescriptionChange,
                onAppearanceChange = viewModel::onAppearanceChange,
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            BuiltInTemplatesSection(
                hasExistingFields = uiState.fields.isNotEmpty(),
                onAdopt = viewModel::onAdoptBuiltIn,
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            FieldListSection(
                fields = uiState.fields,
                onAddField = viewModel::onAddField,
                onEditField = viewModel::onEditField,
                onRemoveField = viewModel::onRemoveField,
                onMoveUp = { idx -> if (idx > 0) viewModel.onReorder(idx, idx - 1) },
                onMoveDown = { idx -> if (idx < uiState.fields.size - 1) viewModel.onReorder(idx, idx + 1) },
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MetadataSection(
    name: String,
    description: String,
    appearance: Appearance,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAppearanceChange: (Appearance) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Details", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = {
                Text(if (name.isBlank()) "Name *" else "Name")
            },
            singleLine = true,
            isError = name.isBlank(),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AppearancePalette.swatches) { swatch ->
                val selected = swatch.color == appearance.color
                val color = parseHexColor(swatch.color)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                            else Modifier
                        )
                        .clickable { onAppearanceChange(swatch) },
                )
            }
        }
    }
}

@Composable
private fun BuiltInTemplatesSection(
    hasExistingFields: Boolean,
    onAdopt: (ReviewTemplate) -> Unit,
) {
    var pendingTemplate by remember { mutableStateOf<ReviewTemplate?>(null) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Built-in templates", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(BuiltInTemplates.all) { template ->
                AssistChip(
                    onClick = {
                        if (hasExistingFields) {
                            pendingTemplate = template
                        } else {
                            onAdopt(template)
                        }
                    },
                    label = { Text(template.displayName()) },
                )
            }
        }
    }

    pendingTemplate?.let { template ->
        AlertDialog(
            onDismissRequest = { pendingTemplate = null },
            title = { Text("Replace current fields?") },
            text = { Text("This will replace all current fields with the \"${template.displayName()}\" template.") },
            confirmButton = {
                TextButton(onClick = {
                    onAdopt(template)
                    pendingTemplate = null
                }) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = { pendingTemplate = null }) { Text("Cancel") }
            },
        )
    }
}

private fun ReviewTemplate.displayName(): String = when (collectionId.value) {
    "__builtin_coffee_ranking__" -> "Coffee Ranking"
    "__builtin_wishlist__" -> "Wishlist"
    "__builtin_geo_diary__" -> "Geo Diary"
    else -> collectionId.value
}

@Composable
private fun FieldListSection(
    fields: ImmutableList<FieldDraft>,
    onAddField: () -> Unit,
    onEditField: (Int, FieldDraft) -> Unit,
    onRemoveField: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Template fields", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

        if (fields.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "No fields yet. Add a field or adopt a built-in template above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onAddField) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add field")
                    }
                }
            }
        } else {
            fields.forEachIndexed { index, draft ->
                TemplateFieldRow(
                    draft = draft,
                    index = index,
                    total = fields.size,
                    onEdit = { editingIndex = index },
                    onRemove = { onRemoveField(index) },
                    onMoveUp = { onMoveUp(index) },
                    onMoveDown = { onMoveDown(index) },
                )
                HorizontalDivider()
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAddField) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add field")
            }
        }
    }

    editingIndex?.let { idx ->
        if (idx < fields.size) {
            TemplateFieldEditorSheet(
                draft = fields[idx],
                onConfirm = { updated ->
                    onEditField(idx, updated)
                    editingIndex = null
                },
                onDismiss = { editingIndex = null },
            )
        }
    }
}

@Composable
private fun TemplateFieldRow(
    draft: FieldDraft,
    index: Int,
    total: Int,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val nameDisplay = when (draft) {
                is FieldDraft.New -> draft.name
                is FieldDraft.Existing -> draft.name
            }
            Text(
                text = draft.label.ifBlank { nameDisplay },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = buildString {
                    append(draft.type.name)
                    val summary = configSummary(draft.config)
                    if (summary.isNotEmpty()) append(" · $summary")
                    if (draft.required) append(" · required")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (index > 0) {
            IconButton(onClick = onMoveUp) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
            }
        }
        if (index < total - 1) {
            IconButton(onClick = onMoveDown) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit field")
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Remove field")
        }
    }
}

private fun configSummary(config: TemplateFieldConfig?): String = when (config) {
    null -> ""
    is TemplateFieldConfig.Score -> "0–${config.max.toInt()} ${config.render}, step ${config.step}"
    is TemplateFieldConfig.Text -> if (config.multiline) "multiline" else "single-line"
    is TemplateFieldConfig.Enum -> {
        val preview = config.options.take(3).joinToString()
        if (config.options.size > 3) "$preview…" else preview
    }
    TemplateFieldConfig.BooleanField -> "boolean"
    TemplateFieldConfig.Date -> "date"
    TemplateFieldConfig.PowerRanking -> "power ranking"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateFieldEditorSheet(
    draft: FieldDraft,
    onConfirm: (FieldDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var label by remember(draft) { mutableStateOf(draft.label) }
    var type by remember(draft) { mutableStateOf(draft.type) }
    var config by remember(draft) { mutableStateOf(draft.config) }
    var required by remember(draft) { mutableStateOf(draft.required) }

    // for New drafts only — name is editable
    var newName by remember(draft) {
        mutableStateOf(if (draft is FieldDraft.New) draft.name else "")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .dismissKeyboardOnTap()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Edit field", style = MaterialTheme.typography.titleMedium)

            // Name row — exhaustive when
            when (draft) {
                is FieldDraft.New -> {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Machine key (snake_case)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is FieldDraft.Existing -> {
                    Column {
                        Text(
                            text = draft.name,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "machine key — cannot change",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            FieldTypeDropdown(
                selected = type,
                onSelect = { newType ->
                    type = newType
                    config = defaultConfigFor(newType)
                },
            )

            FieldConfigEditor(
                type = type,
                config = config,
                onConfigChange = { config = it },
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Required", modifier = Modifier.weight(1f))
                Switch(checked = required, onCheckedChange = { required = it })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val result = when (draft) {
                            is FieldDraft.New -> FieldDraft.New(
                                name = newName.trim().replace(" ", "_").lowercase().ifBlank { draft.name },
                                label = label,
                                type = type,
                                config = config,
                                required = required,
                            )
                            is FieldDraft.Existing -> FieldDraft.Existing(
                                name = draft.name,
                                label = label,
                                type = type,
                                config = config,
                                required = required,
                            )
                        }
                        onConfirm(result)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}

private fun defaultConfigFor(type: FieldType): TemplateFieldConfig? = when (type) {
    FieldType.Score -> TemplateFieldConfig.Score()
    FieldType.Text -> TemplateFieldConfig.Text(multiline = false)
    FieldType.Enum -> TemplateFieldConfig.Enum(options = persistentListOf())
    FieldType.Boolean -> TemplateFieldConfig.BooleanField
    FieldType.Date -> TemplateFieldConfig.Date
    FieldType.PowerRanking -> TemplateFieldConfig.PowerRanking
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldTypeDropdown(
    selected: FieldType,
    onSelect: (FieldType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            FieldType.entries.forEach { ft ->
                DropdownMenuItem(
                    text = { Text(ft.name) },
                    onClick = {
                        onSelect(ft)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldConfigEditor(
    type: FieldType,
    config: TemplateFieldConfig?,
    onConfigChange: (TemplateFieldConfig?) -> Unit,
) {
    when (type) {
        FieldType.Score -> {
            val scoreConfig = config as? TemplateFieldConfig.Score ?: TemplateFieldConfig.Score()
            var minText by remember(scoreConfig) { mutableStateOf(scoreConfig.min.toString()) }
            var maxText by remember(scoreConfig) { mutableStateOf(scoreConfig.max.toString()) }
            var stepText by remember(scoreConfig) { mutableStateOf(scoreConfig.step.toString()) }
            var renderExpanded by remember { mutableStateOf(false) }

            fun emitScore() {
                onConfigChange(scoreConfig.copy(
                    min = minText.toDoubleOrNull() ?: scoreConfig.min,
                    max = maxText.toDoubleOrNull() ?: scoreConfig.max,
                    step = stepText.toDoubleOrNull() ?: scoreConfig.step,
                ))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minText,
                    onValueChange = { minText = it; emitScore() },
                    label = { Text("Min") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = maxText,
                    onValueChange = { maxText = it; emitScore() },
                    label = { Text("Max") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = stepText,
                    onValueChange = { stepText = it; emitScore() },
                    label = { Text("Step") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            ExposedDropdownMenuBox(
                expanded = renderExpanded,
                onExpandedChange = { renderExpanded = !renderExpanded },
            ) {
                OutlinedTextField(
                    value = scoreConfig.render,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Render") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = renderExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = renderExpanded,
                    onDismissRequest = { renderExpanded = false },
                ) {
                    listOf("stars", "number", "bar").forEach { render ->
                        DropdownMenuItem(
                            text = { Text(render) },
                            onClick = {
                                onConfigChange(scoreConfig.copy(render = render))
                                renderExpanded = false
                            },
                        )
                    }
                }
            }
        }
        FieldType.Text -> {
            val textConfig = config as? TemplateFieldConfig.Text ?: TemplateFieldConfig.Text()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Multiline", modifier = Modifier.weight(1f))
                Switch(
                    checked = textConfig.multiline,
                    onCheckedChange = { onConfigChange(textConfig.copy(multiline = it)) },
                )
            }
        }
        FieldType.Enum -> {
            val enumConfig = config as? TemplateFieldConfig.Enum
                ?: TemplateFieldConfig.Enum(options = persistentListOf())
            var newOption by remember { mutableStateOf("") }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Options", style = MaterialTheme.typography.labelMedium)
                enumConfig.options.forEachIndexed { idx, opt ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(opt, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            onConfigChange(enumConfig.copy(
                                options = enumConfig.options.toPersistentList().removeAt(idx)
                            ))
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove option")
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newOption,
                        onValueChange = { newOption = it },
                        label = { Text("New option") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            if (newOption.isNotBlank()) {
                                onConfigChange(enumConfig.copy(
                                    options = enumConfig.options.toPersistentList().add(newOption.trim())
                                ))
                                newOption = ""
                            }
                        },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add option")
                    }
                }
            }
        }
        FieldType.Boolean, FieldType.Date, FieldType.PowerRanking -> {
            // no config UI needed
        }
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        val cleaned = hex.trimStart('#')
        val long = cleaned.toLong(16)
        val r = ((long shr 16) and 0xFF) / 255f
        val g = ((long shr 8) and 0xFF) / 255f
        val b = (long and 0xFF) / 255f
        Color(r, g, b)
    } catch (_: Exception) {
        Color.Gray
    }
}
