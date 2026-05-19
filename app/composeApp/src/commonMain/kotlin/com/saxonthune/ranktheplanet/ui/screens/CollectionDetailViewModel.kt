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
)

class CollectionDetailViewModel(
    collectionId: CollectionId,
    collections: CollectionRepository,
    entries: EntryRepository,
    templates: TemplateRepository,
) : ViewModel() {

    private val _sortMode = MutableStateFlow(SortMode.DateAdded)

    val uiState: StateFlow<CollectionDetailUiState> = combine(
        collections.observe(collectionId),
        entries.observeByCollection(collectionId),
        templates.observe(collectionId),
        _sortMode,
    ) { collection, entryList, template, sortMode ->
        val scoreFieldName = template?.fields
            ?.firstOrNull { it.type == FieldType.Stars || it.type == FieldType.PowerRanking }
            ?.name

        val rows = entryList.map { entry ->
            val score = scoreFieldName
                ?.let { entry.review.data[it] }
                ?.toDoubleOrNull()
            val summary = entry.review.data.entries
                .filter { (k, _) -> k != scoreFieldName }
                .firstOrNull()
                ?.value
                ?.take(60)
                ?: ""
            EntryRowUi(
                id = entry.id,
                displayName = entry.location.displayName,
                reviewSummary = summary,
                score = score,
                added = entry.added,
                reviewLastModified = entry.review.lastModified,
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionDetailUiState())

    fun setSort(mode: SortMode) {
        _sortMode.value = mode
    }
}
