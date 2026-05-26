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

enum class SortMode { DateAdded, ReviewTime, Score, PowerRank }

enum class SortDirection { Ascending, Descending }

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
    val sortDirection: SortDirection = SortDirection.Descending,
    val availableSortModes: ImmutableList<SortMode> = persistentListOf(SortMode.DateAdded, SortMode.ReviewTime),
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
    private val _sortDirection = MutableStateFlow(SortDirection.Descending)
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

    fun setSortDirection(direction: SortDirection) {
        _sortDirection.value = direction
    }

    fun toggleSortDirection() {
        _sortDirection.update { if (it == SortDirection.Descending) SortDirection.Ascending else SortDirection.Descending }
    }

    private fun load() {
        viewModelScope.launch {
            try {
                combine(
                    collections.observe(collectionId),
                    entries.observeByCollection(collectionId),
                    templates.observe(collectionId),
                    _sortMode,
                    _sortDirection,
                ) { collection, entryList, template, sortMode, sortDirection ->
                    val scoreFieldName = template?.fields
                        ?.firstOrNull { it.type == FieldType.Score }
                        ?.name

                    val availableSortModes = buildList {
                        add(SortMode.DateAdded)
                        add(SortMode.ReviewTime)
                        if (scoreFieldName != null) add(SortMode.Score)
                        if (collection?.powerRanking == true) add(SortMode.PowerRank)
                    }.toImmutableList()

                    val effectiveSortMode = if (sortMode in availableSortModes) sortMode else SortMode.DateAdded

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

                    val sorted = when (effectiveSortMode) {
                        SortMode.PowerRank -> rows.sortedBy { it.added }
                        SortMode.DateAdded -> {
                            val base = rows.sortedByDescending { it.added }
                            if (sortDirection == SortDirection.Ascending) base.reversed() else base
                        }
                        SortMode.ReviewTime -> {
                            val base = rows.sortedByDescending { it.reviewLastModified }
                            if (sortDirection == SortDirection.Ascending) base.reversed() else base
                        }
                        SortMode.Score -> {
                            val base = rows.sortedWith(
                                compareByDescending<EntryRowUi> { it.score != null }
                                    .thenByDescending { it.score ?: 0.0 }
                            )
                            if (sortDirection == SortDirection.Ascending) base.reversed() else base
                        }
                    }

                    CollectionDetailUiState(
                        collection = collection,
                        entries = sorted.toImmutableList(),
                        scoreFieldName = scoreFieldName,
                        sortMode = effectiveSortMode,
                        sortDirection = sortDirection,
                        availableSortModes = availableSortModes,
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
