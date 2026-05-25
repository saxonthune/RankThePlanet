package com.saxonthune.ranktheplanet.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.domain.Appearance
import com.saxonthune.ranktheplanet.domain.AppearancePalette
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.FieldType
import com.saxonthune.ranktheplanet.domain.ReviewTemplate
import com.saxonthune.ranktheplanet.domain.TemplateField
import com.saxonthune.ranktheplanet.domain.TemplateFieldConfig
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface EditorMode {
    data object Create : EditorMode
    data class Edit(val id: CollectionId) : EditorMode
}

sealed interface FieldDraft {
    val label: String
    val type: FieldType
    val config: TemplateFieldConfig?

    data class New(
        override val label: String,
        override val type: FieldType,
        override val config: TemplateFieldConfig?,
    ) : FieldDraft

    data class Existing(
        val name: String,
        override val label: String,
        override val type: FieldType,
        override val config: TemplateFieldConfig?,
    ) : FieldDraft
}

const val DATE_FIELD_NAME = "date"
val DATE_FIELD: TemplateField = TemplateField(
    name = DATE_FIELD_NAME,
    label = "Date",
    type = FieldType.Date,
    config = TemplateFieldConfig.Date,
    ordinal = 0,
)

internal fun sluggify(label: String, taken: Set<String>): String {
    val base = label.trim().lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "field" }
    if (base !in taken && base != DATE_FIELD_NAME) return base
    var i = 2
    while (true) {
        val candidate = "${base}_$i"
        if (candidate !in taken && candidate != DATE_FIELD_NAME) return candidate
        i++
    }
}

data class CollectionEditorUiState(
    val mode: EditorMode = EditorMode.Create,
    val name: String = "",
    val description: String = "",
    val appearance: Appearance = AppearancePalette.default,
    val powerRanking: Boolean = false,
    val fields: ImmutableList<FieldDraft> = persistentListOf(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val canSave: Boolean get() = name.isNotBlank() && !isSaving
}

sealed interface CollectionEditorEvent {
    data class Saved(val id: CollectionId) : CollectionEditorEvent
}

class CollectionEditorViewModel(
    private val mode: EditorMode,
    private val collections: CollectionRepository,
    private val templates: TemplateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionEditorUiState(mode = mode))
    val uiState: StateFlow<CollectionEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<CollectionEditorEvent>(Channel.BUFFERED)
    val events: Flow<CollectionEditorEvent> = _events.receiveAsFlow()

    init {
        if (mode is EditorMode.Edit) {
            viewModelScope.launch { load(mode.id) }
        }
    }

    private suspend fun load(id: CollectionId) {
        _uiState.update { it.copy(isLoading = true) }
        combine(
            collections.observe(id),
            templates.observe(id),
        ) { collection, template -> Pair(collection, template) }
            .collect { (collection, template) ->
                if (collection == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Collection not found") }
                    return@collect
                }
                val drafts: PersistentList<FieldDraft> = template?.fields
                    ?.filter { it.name != DATE_FIELD_NAME }
                    ?.map { field ->
                        FieldDraft.Existing(
                            name = field.name,
                            label = field.label,
                            type = field.type,
                            config = field.config,
                        )
                    }
                    ?.toPersistentList()
                    ?: persistentListOf()
                _uiState.update {
                    it.copy(
                        name = collection.name,
                        description = collection.description ?: "",
                        appearance = collection.appearance,
                        powerRanking = collection.powerRanking,
                        fields = drafts,
                        isLoading = false,
                        error = null,
                    )
                }
            }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, error = null) }

    fun onDescriptionChange(description: String) = _uiState.update { it.copy(description = description) }

    fun onAppearanceChange(appearance: Appearance) = _uiState.update { it.copy(appearance = appearance) }

    fun onPowerRankingChange(enabled: Boolean) = _uiState.update { it.copy(powerRanking = enabled) }

    fun onAddField(): Int {
        var newIndex = 0
        _uiState.update { state ->
            val newDraft = FieldDraft.New(
                label = "",
                type = FieldType.Text,
                config = TemplateFieldConfig.TextField,
            )
            val updated = state.fields.toPersistentList().add(newDraft)
            newIndex = updated.size - 1
            state.copy(fields = updated)
        }
        return newIndex
    }

    fun onEditField(index: Int, draft: FieldDraft) {
        _uiState.update { state ->
            val updated = state.fields.toPersistentList().mutate { it[index] = draft }
            state.copy(fields = updated)
        }
    }

    fun onRemoveField(index: Int) {
        _uiState.update { state ->
            state.copy(fields = state.fields.toPersistentList().removeAt(index))
        }
    }

    fun onReorder(from: Int, to: Int) {
        _uiState.update { state ->
            val list = state.fields.toPersistentList().mutate { mutable ->
                val item = mutable.removeAt(from)
                mutable.add(to, item)
            }
            state.copy(fields = list)
        }
    }

    fun onAdoptBuiltIn(template: ReviewTemplate) {
        val drafts = template.fields
            .filter { it.name != DATE_FIELD_NAME }
            .map { field ->
                FieldDraft.New(
                    label = field.label,
                    type = field.type,
                    config = field.config,
                )
            }
            .toPersistentList()
        _uiState.update { it.copy(fields = drafts) }
    }

    private fun buildFieldList(drafts: List<FieldDraft>): List<TemplateField> {
        val taken = mutableSetOf<String>()
        drafts.filterIsInstance<FieldDraft.Existing>().forEach { taken += it.name }
        val userFields = drafts.mapIndexed { idx, draft ->
            when (draft) {
                is FieldDraft.Existing -> TemplateField(
                    name = draft.name,
                    label = draft.label,
                    type = draft.type,
                    config = draft.config,
                    ordinal = idx + 1,
                )
                is FieldDraft.New -> {
                    val name = sluggify(draft.label, taken)
                    taken += name
                    TemplateField(
                        name = name,
                        label = draft.label.ifBlank { name },
                        type = draft.type,
                        config = draft.config,
                        ordinal = idx + 1,
                    )
                }
            }
        }
        return listOf(DATE_FIELD) + userFields
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val fieldList = buildFieldList(state.fields)
            when (mode) {
                EditorMode.Create -> {
                    collections.create(
                        name = state.name,
                        description = state.description.ifBlank { null },
                        appearance = state.appearance,
                        powerRanking = state.powerRanking,
                    ).onFailure { e ->
                        _uiState.update { it.copy(isSaving = false, error = e.message ?: "Save failed") }
                        return@launch
                    }.onSuccess { collection ->
                        templates.define(collection.id, fieldList).onFailure { e ->
                            _uiState.update { it.copy(isSaving = false, error = e.message ?: "Failed to save template") }
                            return@launch
                        }
                        _events.send(CollectionEditorEvent.Saved(collection.id))
                    }
                }
                is EditorMode.Edit -> {
                    collections.editMetadata(
                        id = mode.id,
                        name = state.name,
                        description = state.description.ifBlank { null },
                        appearance = state.appearance,
                        powerRanking = state.powerRanking,
                    ).onFailure { e ->
                        _uiState.update { it.copy(isSaving = false, error = e.message ?: "Save failed") }
                        return@launch
                    }
                    templates.edit(mode.id, fieldList).onFailure { e ->
                        _uiState.update { it.copy(isSaving = false, error = e.message ?: "Failed to save template") }
                        return@launch
                    }
                    _events.send(CollectionEditorEvent.Saved(mode.id))
                }
            }
        }
    }
}
