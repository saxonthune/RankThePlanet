package com.saxonthune.ranktheplanet.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.domain.Entry
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.FieldType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewFieldUi(
    val label: String,
    val value: String,
    val type: FieldType,
    val isSet: Boolean,
)

data class CollectionEntryDetailUiState(
    val entry: Entry? = null,
    val fields: ImmutableList<ReviewFieldUi> = persistentListOf(),
    val reviewed: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionEntryDetailViewModel(
    private val entryId: EntryId,
    private val entries: EntryRepository,
    private val templates: TemplateRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CollectionEntryDetailUiState())
    val uiState: StateFlow<CollectionEntryDetailUiState> = _state

    init {
        load()
    }

    fun retry() {
        _state.update { it.copy(error = null, isLoading = true) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                entries.observe(entryId)
                    .flatMapLatest { entry ->
                        if (entry == null) {
                            flowOf(entry to null)
                        } else {
                            templates.observe(entry.collectionId).map { template -> entry to template }
                        }
                    }
                    .map { (entry, template) ->
                        val fields = template?.fields?.map { field ->
                            val value = entry?.review?.data?.get(field.name) ?: ""
                            ReviewFieldUi(
                                label = field.name,
                                value = value,
                                type = field.type,
                                isSet = value.isNotBlank(),
                            )
                        }?.toImmutableList() ?: persistentListOf()

                        CollectionEntryDetailUiState(
                            entry = entry,
                            fields = fields,
                            reviewed = entry?.review != null,
                            isLoading = false,
                        )
                    }
                    .collect { next ->
                        _state.value = next
                    }
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, error = t.message ?: "Unknown error") }
            }
        }
    }
}
