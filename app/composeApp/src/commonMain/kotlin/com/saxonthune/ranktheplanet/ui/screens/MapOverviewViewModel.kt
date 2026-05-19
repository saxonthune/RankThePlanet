package com.saxonthune.ranktheplanet.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.location.LocationProvider
import com.saxonthune.ranktheplanet.data.location.ProviderResult
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.EntryId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PinUi(
    val entryId: EntryId,
    val lat: Double,
    val lng: Double,
    val color: String,
    val visited: Boolean,
)

data class CollectionFilterRowUi(
    val id: CollectionId,
    val name: String,
    val color: String,
    val shown: Boolean,
)

data class SearchResultUi(
    val displayName: String,
    val lat: Double,
    val lng: Double,
)

data class MapOverviewUiState(
    val pins: ImmutableList<PinUi> = persistentListOf(),
    val collectionRows: ImmutableList<CollectionFilterRowUi> = persistentListOf(),
    val searchResults: ImmutableList<SearchResultUi> = persistentListOf(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = true,
)

class MapOverviewViewModel(
    collections: CollectionRepository,
    entries: EntryRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val hiddenCollections = MutableStateFlow<Set<CollectionId>>(emptySet())
    private val searchResults = MutableStateFlow<List<SearchResultUi>>(emptyList())
    private val isSearching = MutableStateFlow(false)

    val uiState: StateFlow<MapOverviewUiState> = combine(
        collections.observeAll(),
        entries.observeAll(),
        hiddenCollections,
        searchResults,
        isSearching,
    ) { collectionList, entryList, hidden, results, searching ->
        val collectionMap = collectionList.associateBy { it.id }
        val collectionRows = collectionList.map { col ->
            CollectionFilterRowUi(
                id = col.id,
                name = col.name,
                color = col.appearance.color,
                shown = col.id !in hidden,
            )
        }.toImmutableList()
        val pins = entryList
            .filter { it.collectionId !in hidden }
            .mapNotNull { entry ->
                val col = collectionMap[entry.collectionId] ?: return@mapNotNull null
                PinUi(
                    entryId = entry.id,
                    lat = entry.location.coordinates.lat,
                    lng = entry.location.coordinates.lng,
                    color = col.appearance.color,
                    visited = entry.review != null,
                )
            }.toImmutableList()
        MapOverviewUiState(
            pins = pins,
            collectionRows = collectionRows,
            searchResults = results.toImmutableList(),
            isSearching = searching,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapOverviewUiState())

    fun toggleCollection(id: CollectionId) {
        hiddenCollections.value = hiddenCollections.value.let { hidden ->
            if (id in hidden) hidden - id else hidden + id
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            isSearching.value = true
            val result = locationProvider.resolve(query)
            searchResults.value = when (result) {
                is ProviderResult.Ok -> result.value.map { candidate ->
                    SearchResultUi(
                        displayName = candidate.displayName,
                        lat = candidate.coordinates.lat,
                        lng = candidate.coordinates.lng,
                    )
                }
                is ProviderResult.Failed -> emptyList()
            }
            isSearching.value = false
        }
    }
}
