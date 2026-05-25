package com.saxonthune.ranktheplanet.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.domain.Collection
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.FieldType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SortMode { DateAdded, ReviewTime, Score }

data class EntryRowUi(
    val id: EntryId,
    val displayName: String,
    val reviewSummary: String,
    val score: Double?,
    val added: String,
    val reviewLastModified: String,
)

data class CollectionDetailUiState(
    val collection: Collection? = null,
    val entries: ImmutableList<EntryRowUi> = persistentListOf(),
    val scoreFieldName: String? = null,
    val sortMode: SortMode = SortMode.DateAdded,
    val isLoading: Boolean = true,
    val error: String? = null,
)

class CollectionDetailViewModel(
    private val collectionId: CollectionId,
    private val collections: CollectionRepository,
    private val entries: EntryRepository,
    private val templates: TemplateRepository,
) : ViewModel() {

    private val _sortMode = MutableStateFlow(SortMode.DateAdded)
    private val _state = MutableStateFlow(CollectionDetailUiState())
    val uiState: StateFlow<CollectionDetailUiState> = _state

    init {
        load()
    }

    fun retry() {
        _state.update { it.copy(error = null, isLoading = true) }
        load()
    }

    fun setSort(mode: SortMode) {
        _sortMode.value = mode
    }

    private fun load() {
        viewModelScope.launch {
            try {
                combine(
                    collections.observe(collectionId),
                    entries.observeByCollection(collectionId),
                    templates.observe(collectionId),
                    _sortMode,
                ) { collection, entryList, template, sortMode ->
                    val scoreFieldName = template?.fields
                        ?.firstOrNull { it.type == FieldType.Score }
                        ?.name

                    val rows = entryList.map { entry ->
                        val score = scoreFieldName
                            ?.let { entry.review?.data?.get(it) }
                            ?.toDoubleOrNull()
                        val summary = entry.review?.data?.entries
                            ?.filter { (k, _) -> k != scoreFieldName }
                            ?.firstOrNull()
                            ?.value
                            ?.take(60)
                            ?: ""
                        EntryRowUi(
                            id = entry.id,
                            displayName = entry.location.displayName,
                            reviewSummary = summary,
                            score = score,
                            added = entry.added,
                            reviewLastModified = entry.review?.lastModified ?: "",
                        )
                    }

                    val sorted = when (sortMode) {
                        SortMode.DateAdded -> rows.sortedByDescending { it.added }
                        SortMode.ReviewTime -> rows.sortedByDescending { it.reviewLastModified }
                        SortMode.Score -> rows.sortedWith(
                            compareByDescending<EntryRowUi> { it.score != null }
                                .thenByDescending { it.score ?: 0.0 }
                        )
                    }

                    CollectionDetailUiState(
                        collection = collection,
                        entries = sorted.toImmutableList(),
                        scoreFieldName = scoreFieldName,
                        sortMode = sortMode,
                        isLoading = false,
                    )
                }.collect { next ->
                    _state.value = next
                }
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, error = t.message ?: "Unknown error") }
            }
        }
    }
}
