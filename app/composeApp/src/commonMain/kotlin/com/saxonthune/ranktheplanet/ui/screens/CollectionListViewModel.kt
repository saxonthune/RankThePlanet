package com.saxonthune.ranktheplanet.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.domain.CollectionId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CollectionRowUi(
    val id: CollectionId,
    val name: String,
    val color: String,
    val pinStyle: String,
    val entryCount: Int,
)

data class CollectionListUiState(
    val collections: ImmutableList<CollectionRowUi> = persistentListOf(),
    val isLoading: Boolean = true,
)

class CollectionListViewModel(
    collections: CollectionRepository,
    entries: EntryRepository,
) : ViewModel() {

    val uiState: StateFlow<CollectionListUiState> = combine(
        collections.observeAll(),
        entries.observeAll(),
    ) { collectionList, entryList ->
        val countByCollection = entryList
            .groupingBy { it.collectionId }
            .eachCount()
        val rows = collectionList.map { col ->
            CollectionRowUi(
                id = col.id,
                name = col.name,
                color = col.appearance.color,
                pinStyle = col.appearance.pinStyle,
                entryCount = countByCollection.getOrElse(col.id) { 0 },
            )
        }.toImmutableList()
        CollectionListUiState(collections = rows, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionListUiState())
}
