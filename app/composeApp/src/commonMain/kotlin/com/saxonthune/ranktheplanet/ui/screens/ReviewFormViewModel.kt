package com.saxonthune.ranktheplanet.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.TemplateField
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewFormUiState(
    val title: String = "",
    val fields: ImmutableList<TemplateField> = persistentListOf(),
    val draft: PersistentMap<String, String> = persistentMapOf(),
    val currentTemplateVersion: Int = 0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
)

sealed interface ReviewFormEvent {
    data object Saved : ReviewFormEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewFormViewModel(
    private val entryId: EntryId,
    private val entries: EntryRepository,
    private val collections: CollectionRepository,
    private val templates: TemplateRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewFormUiState())
    val uiState: StateFlow<ReviewFormUiState> = _state.asStateFlow()

    private val _events = Channel<ReviewFormEvent>(Channel.BUFFERED)
    val events: Flow<ReviewFormEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { load() }
    }

    fun onEdit(name: String, value: String) =
        _state.update { it.copy(draft = it.draft.put(name, value), error = null) }

    fun onClear(name: String) =
        _state.update { it.copy(draft = it.draft.remove(name), error = null) }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            entries.editReview(entryId, s.draft, s.currentTemplateVersion)
                .onSuccess { _events.send(ReviewFormEvent.Saved) }
                .onFailure { e -> _state.update { it.copy(isSaving = false, error = e.message ?: "Save failed") } }
        }
    }

    private suspend fun load() {
        entries.observe(entryId)
            .flatMapLatest { entry ->
                if (entry == null) flowOf(Triple(null, null, null))
                else combine(
                    flowOf(entry),
                    collections.observe(entry.collectionId),
                    templates.observe(entry.collectionId),
                ) { e, c, t -> Triple(e, c, t) }
            }
            .collect { (entry, _, template) ->
                if (entry == null) {
                    _state.value = ReviewFormUiState(isLoading = false, error = "Entry not found")
                    return@collect
                }
                if (template == null) {
                    _state.value = ReviewFormUiState(
                        title = entry.location.displayName,
                        isLoading = false,
                        error = "No template for this collection",
                    )
                    return@collect
                }
                val existing = entry.review
                if (existing != null && existing.recordedTemplateVersion != template.version) {
                    _state.value = ReviewFormUiState(
                        title = entry.location.displayName,
                        currentTemplateVersion = template.version,
                        isLoading = false,
                        error = "Template version mismatch (recorded=${existing.recordedTemplateVersion}, current=${template.version}). Pre-alpha: migration is not supported.",
                    )
                    return@collect
                }
                _state.value = ReviewFormUiState(
                    title = entry.location.displayName,
                    fields = template.fields,
                    draft = (existing?.data ?: persistentMapOf()).toPersistentMap(),
                    currentTemplateVersion = template.version,
                    isLoading = false,
                )
            }
    }
}
