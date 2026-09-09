package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.ui.theme.parseAppearanceColor
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

data class CollectionRowUi(
    val id: CollectionId,
    val name: String,
    val color: Color,
    val pinStyle: String,
    val entryCount: Int,
)

data class CollectionListUiState(
    val collections: ImmutableList<CollectionRowUi> = persistentListOf(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class CollectionListViewModel(
    private val collections: CollectionRepository,
    private val entries: EntryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CollectionListUiState())
    val uiState: StateFlow<CollectionListUiState> = _state

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
                combine(
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
                            color = parseAppearanceColor(col.appearance.color),
                            pinStyle = col.appearance.pinStyle,
                            entryCount = countByCollection.getOrElse(col.id) { 0 },
                        )
                    }.toImmutableList()
                    CollectionListUiState(collections = rows, isLoading = false)
                }.collect { next ->
                    _state.value = next
                }
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, error = t.message ?: "Unknown error") }
            }
        }
    }
}
