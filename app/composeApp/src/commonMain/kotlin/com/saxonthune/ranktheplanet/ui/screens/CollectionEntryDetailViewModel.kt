package com.saxonthune.ranktheplanet.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.domain.Collection
import com.saxonthune.ranktheplanet.domain.Entry
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.FieldType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    val collection: Collection? = null,
    val fields: ImmutableList<ReviewFieldUi> = persistentListOf(),
    val reviewed: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionEntryDetailViewModel(
    private val entryId: EntryId,
    private val entries: EntryRepository,
    private val collections: CollectionRepository,
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
                            flowOf(Triple<Entry?, Collection?, com.saxonthune.ranktheplanet.domain.ReviewTemplate?>(null, null, null))
                        } else {
                            combine(
                                collections.observe(entry.collectionId),
                                templates.observe(entry.collectionId),
                            ) { collection, template -> Triple(entry, collection, template) }
                        }
                    }
                    .collect { (entry, collection, template) ->
                        val fields = template?.fields?.map { field ->
                            val value = entry?.review?.data?.get(field.name) ?: ""
                            ReviewFieldUi(
                                label = field.label,
                                value = value,
                                type = field.type,
                                isSet = value.isNotBlank(),
                            )
                        }?.toImmutableList() ?: persistentListOf()

                        _state.value = CollectionEntryDetailUiState(
                            entry = entry,
                            collection = collection,
                            fields = fields,
                            reviewed = entry?.review != null,
                            isLoading = false,
                        )
                    }
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, error = t.message ?: "Unknown error") }
            }
        }
    }
}
