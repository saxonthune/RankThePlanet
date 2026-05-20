package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.location.LocationProvider
import com.saxonthune.ranktheplanet.data.location.ProviderResult
import com.saxonthune.ranktheplanet.domain.Collection
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.Entry
import com.saxonthune.ranktheplanet.domain.EntryId
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

data class EntrySummaryUi(
    val entryId: EntryId,
    val collectionId: CollectionId,
    val collectionName: String,
    val collectionColor: Color,
    val locationName: String,
    val visited: Boolean,
)

sealed interface PinSheet {
    data object None : PinSheet
    data class Peek(
        val locationName: String,
        val lat: Double,
        val lng: Double,
        val entries: ImmutableList<EntrySummaryUi>,
    ) : PinSheet
    data class Entry(val entry: EntrySummaryUi) : PinSheet
}

data class PinUi(
    val entryId: EntryId,
    val locationName: String,
    val lat: Double,
    val lng: Double,
    val color: Color,
    val visited: Boolean,
)

data class CollectionFilterRowUi(
    val id: CollectionId,
    val name: String,
    val color: Color,
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
    val pinSheet: PinSheet = PinSheet.None,
    val error: String? = null,
)

class MapOverviewViewModel(
    private val collectionsRepo: CollectionRepository,
    private val entriesRepo: EntryRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val hiddenCollections = MutableStateFlow<Set<CollectionId>>(emptySet())
    private val searchResults = MutableStateFlow<List<SearchResultUi>>(emptyList())
    private val isSearching = MutableStateFlow(false)
    private val _pinSheet = MutableStateFlow<PinSheet>(PinSheet.None)
    private val _error = MutableStateFlow<String?>(null)

    private val _latestCollections = MutableStateFlow<List<Collection>>(emptyList())
    private val _latestEntries = MutableStateFlow<List<Entry>>(emptyList())

    init {
        loadData()
    }

    fun retry() {
        _error.value = null
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                collectionsRepo.observeAll().collect { _latestCollections.value = it }
            } catch (t: Throwable) {
                _error.value = t.message ?: "Unknown error"
            }
        }
        viewModelScope.launch {
            try {
                entriesRepo.observeAll().collect { _latestEntries.value = it }
            } catch (t: Throwable) {
                _error.value = t.message ?: "Unknown error"
            }
        }
    }

    private data class DerivedBase(
        val pins: ImmutableList<PinUi>,
        val collectionRows: ImmutableList<CollectionFilterRowUi>,
    )

    private val derivedBase: StateFlow<DerivedBase> = combine(
        collectionsRepo.observeAll(),
        entriesRepo.observeAll(),
        hiddenCollections,
    ) { collectionList, entryList, hidden ->
        val collectionMap = collectionList.associateBy { it.id }
        val collectionRows = collectionList.map { col ->
            CollectionFilterRowUi(
                id = col.id,
                name = col.name,
                color = parseAppearanceColor(col.appearance.color),
                shown = col.id !in hidden,
            )
        }.toImmutableList()
        val pins = entryList
            .filter { it.collectionId !in hidden }
            .mapNotNull { entry ->
                val col = collectionMap[entry.collectionId] ?: return@mapNotNull null
                PinUi(
                    entryId = entry.id,
                    locationName = entry.location.displayName,
                    lat = entry.location.coordinates.lat,
                    lng = entry.location.coordinates.lng,
                    color = parseAppearanceColor(col.appearance.color),
                    visited = entry.review != null,
                )
            }.toImmutableList()
        DerivedBase(pins, collectionRows)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DerivedBase(persistentListOf(), persistentListOf()),
    )

    val uiState: StateFlow<MapOverviewUiState> = combine(
        derivedBase,
        searchResults,
        isSearching,
        _pinSheet,
        _error,
    ) { base, results, searching, sheet, error ->
        MapOverviewUiState(
            pins = base.pins,
            collectionRows = base.collectionRows,
            searchResults = results.toImmutableList(),
            isSearching = searching,
            isLoading = false,
            pinSheet = sheet,
            error = error,
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

    fun selectPin(entryId: EntryId) {
        val entries = _latestEntries.value
        val collections = _latestCollections.value
        val collectionMap = collections.associateBy { it.id }
        val entry = entries.find { it.id == entryId } ?: run {
            _pinSheet.value = PinSheet.None
            return
        }
        val locationId = entry.location.id
        val locationEntries = entries.filter { it.location.id == locationId }
        val summaries = locationEntries.mapNotNull { e ->
            val col = collectionMap[e.collectionId] ?: return@mapNotNull null
            EntrySummaryUi(
                entryId = e.id,
                collectionId = e.collectionId,
                collectionName = col.name,
                collectionColor = parseAppearanceColor(col.appearance.color),
                locationName = e.location.displayName,
                visited = e.review != null,
            )
        }.toImmutableList()
        _pinSheet.value = if (summaries.size == 1) {
            PinSheet.Entry(summaries.first())
        } else {
            PinSheet.Peek(
                locationName = entry.location.displayName,
                lat = entry.location.coordinates.lat,
                lng = entry.location.coordinates.lng,
                entries = summaries,
            )
        }
    }

    fun openEntryFromPeek(entryId: EntryId) {
        val peek = _pinSheet.value as? PinSheet.Peek ?: return
        val summary = peek.entries.find { it.entryId == entryId } ?: return
        _pinSheet.value = PinSheet.Entry(summary)
    }

    fun dismissSheet() {
        _pinSheet.value = PinSheet.None
    }
}
