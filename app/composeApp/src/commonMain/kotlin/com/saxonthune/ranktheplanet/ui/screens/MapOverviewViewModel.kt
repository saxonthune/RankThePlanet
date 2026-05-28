package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.LocationRepository
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.data.location.LocationBias
import com.saxonthune.ranktheplanet.data.location.LocationCandidate
import com.saxonthune.ranktheplanet.data.location.LocationProviderRegistry
import com.saxonthune.ranktheplanet.data.location.ProviderResult
import com.saxonthune.ranktheplanet.data.projection.OverviewProjection
import com.saxonthune.ranktheplanet.data.session.SessionStateStore
import com.saxonthune.ranktheplanet.domain.Collection
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.Entry
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.Location
import com.saxonthune.ranktheplanet.domain.LocationId
import com.saxonthune.ranktheplanet.domain.ReviewDraft
import com.saxonthune.ranktheplanet.domain.ReviewTemplate
import com.saxonthune.ranktheplanet.domain.SourceType
import com.saxonthune.ranktheplanet.domain.Viewport
import com.saxonthune.ranktheplanet.ui.theme.darkenHex
import com.saxonthune.ranktheplanet.ui.theme.parseAppearanceColor
import com.saxonthune.ranktheplanet.util.formatShortDate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EntrySummaryUi(
    val entryId: EntryId,
    val collectionId: CollectionId,
    val collectionName: String,
    val collectionColor: Color,
    val locationName: String,
    val locationAddress: String?,
    val reviewedDate: String?,
    val summaryPreview: String?,
) {
    val reviewed: Boolean get() = reviewedDate != null
}

sealed interface PinSheet {
    data object None : PinSheet
    data class Peek(
        val locationName: String,
        val lat: Double,
        val lng: Double,
        val entries: ImmutableList<EntrySummaryUi>,
        val candidateLocation: Location? = null,
        val addToCollection: CollectionPickRowUi? = null,
        val detail: String? = null,
    ) : PinSheet
    data class Entry(val entry: EntrySummaryUi) : PinSheet
}

sealed interface LocationDraftSheet {
    data object None : LocationDraftSheet
    data class Open(
        val lat: Double,
        val lng: Double,
        val displayName: String? = null,
        val adoptedCandidate: String? = null,
        val manualName: String = "",
        val phase: DraftPhase = DraftPhase.Draft,
        val existingLocation: Location? = null,
    ) : LocationDraftSheet
}

enum class DraftPhase { Draft, AddToCollection }

sealed interface PendingReviewPrompt {
    data object None : PendingReviewPrompt
    data class Pending(
        val entryId: EntryId,
        val locationName: String,
    ) : PendingReviewPrompt
}

data class CollectionPickRowUi(
    val id: CollectionId,
    val name: String,
    val color: Color,
    val entryCount: Int,
)

enum class PinKind { Unreviewed, Reviewed, Multi, MultiUnreviewed }

data class PinUi(
    val entryId: EntryId,
    val collectionId: CollectionId,
    val locationId: LocationId,
    val locationName: String,
    val lat: Double,
    val lng: Double,
    val color: Color,
    val colorHex: String,
    val darkColorHex: String,
    val kind: PinKind,
)

data class CollectionFilterRowUi(
    val id: CollectionId,
    val name: String,
    val color: Color,
    val shown: Boolean,
)

sealed interface SearchHitUi {
    val displayName: String
    val lat: Double
    val lng: Double

    data class ExistingEntry(
        override val displayName: String,
        override val lat: Double,
        override val lng: Double,
        val locationId: LocationId,
        val dots: ImmutableList<Color>,
    ) : SearchHitUi

    data class Candidate(
        override val displayName: String,
        override val lat: Double,
        override val lng: Double,
        val sourceType: SourceType,
        val sourceId: String?,
        val detail: String?,
        val needsConfirmation: Boolean = false,
    ) : SearchHitUi
}

data class NearbyCandidateUi(
    val displayName: String,
    val lat: Double,
    val lng: Double,
    val detail: String?,
)

data class SearchContext(
    val query: String,
    val candidates: ImmutableList<SearchHitUi.Candidate>,
    val viewportAtQuery: Viewport,
)

data class MapOverviewUiState(
    val pins: ImmutableList<PinUi> = persistentListOf(),
    val collectionRows: ImmutableList<CollectionFilterRowUi> = persistentListOf(),
    val collectionPicks: ImmutableList<CollectionPickRowUi> = persistentListOf(),
    val searchHits: ImmutableList<SearchHitUi> = persistentListOf(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = true,
    val pinSheet: PinSheet = PinSheet.None,
    val draft: LocationDraftSheet = LocationDraftSheet.None,
    val nearbyCandidates: ImmutableList<NearbyCandidateUi> = persistentListOf(),
    val isResolvingNearby: Boolean = false,
    val error: String? = null,
    val pendingReview: PendingReviewPrompt = PendingReviewPrompt.None,
    val viewport: Viewport? = null,
    val searchContext: SearchContext? = null,
    val showSearchThisAreaChip: Boolean = false,
    val isSearchingArea: Boolean = false,
    val filterContext: Set<CollectionId>? = null,
    val collectionListSheet: CollectionListSheet = CollectionListSheet.Closed,
) {
    val isSearchResultsMode: Boolean get() = searchContext != null
}

sealed interface CollectionListSheet {
    data object Closed : CollectionListSheet
    data class Open(val preselection: Set<CollectionId> = emptySet()) : CollectionListSheet
}

@OptIn(ExperimentalCoroutinesApi::class)
class MapOverviewViewModel(
    private val collectionsRepo: CollectionRepository,
    private val entriesRepo: EntryRepository,
    private val providerRegistry: LocationProviderRegistry,
    private val templatesRepo: TemplateRepository,
    private val projection: OverviewProjection,
    private val session: SessionStateStore,
    private val locationsRepo: LocationRepository,
) : ViewModel() {

    private val _restoredViewport = MutableStateFlow<Viewport?>(null)
    private val _liveViewport = MutableStateFlow<Viewport?>(null)
    private val _liveBbox = MutableStateFlow<LocationBias.Box?>(null)
    private val _filterContext = MutableStateFlow<Set<CollectionId>?>(null)
    private val _collectionListSheet = MutableStateFlow<CollectionListSheet>(CollectionListSheet.Closed)
    private val _isSearching = MutableStateFlow(false)
    private val _pinSheet = MutableStateFlow<PinSheet>(PinSheet.None)
    private val _draft = MutableStateFlow<LocationDraftSheet>(LocationDraftSheet.None)
    private val _pendingReview = MutableStateFlow<PendingReviewPrompt>(PendingReviewPrompt.None)
    private val _nearbyCandidates = MutableStateFlow<List<NearbyCandidateUi>>(emptyList())
    private val _isResolvingNearby = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _searchContext = MutableStateFlow<SearchContext?>(null)
    private val _isSearchingArea = MutableStateFlow(false)

    private val _latestCollections = MutableStateFlow<List<Collection>>(emptyList())
    private val _latestEntries = MutableStateFlow<List<Entry>>(emptyList())
    private val _latestTemplates = MutableStateFlow<List<ReviewTemplate>>(emptyList())

    private val _query = MutableStateFlow("")
    private val _submit = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val _jumpToViewport = MutableSharedFlow<Viewport>(replay = 0, extraBufferCapacity = 1)
    val jumpToViewport: SharedFlow<Viewport> = _jumpToViewport.asSharedFlow()

    val pinController = PinRenderController()

    init {
        viewModelScope.launch {
            _restoredViewport.value = projection.loadOverview().viewport
        }
        loadData()
    }

    fun retry() {
        _error.value = null
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                collectionsRepo.observeAll().collect { collections ->
                    _latestCollections.value = collections
                    collections.forEach { col ->
                        viewModelScope.launch {
                            templatesRepo.observe(col.id).collect { template ->
                                if (template != null) {
                                    _latestTemplates.update { current ->
                                        val without = current.filter { it.collectionId != template.collectionId }
                                        without + template
                                    }
                                }
                            }
                        }
                    }
                }
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
        val collectionPicks: ImmutableList<CollectionPickRowUi>,
    )

    private val derivedBase: StateFlow<DerivedBase> = combine(
        collectionsRepo.observeAll(),
        entriesRepo.observeAll(),
        _filterContext,
    ) { collectionList, entryList, filterCtx ->
        val collectionMap = collectionList.associateBy { it.id }
        val entryCounts = entryList.groupingBy { it.collectionId }.eachCount()
        val collectionRows = collectionList.map { col ->
            CollectionFilterRowUi(
                id = col.id,
                name = col.name,
                color = parseAppearanceColor(col.appearance.color),
                shown = true,
            )
        }.toImmutableList()
        val collectionPicks = collectionList.map { col ->
            CollectionPickRowUi(
                id = col.id,
                name = col.name,
                color = parseAppearanceColor(col.appearance.color),
                entryCount = entryCounts[col.id] ?: 0,
            )
        }.toImmutableList()
        val visibleEntries = if (filterCtx == null) entryList else entryList.filter { it.collectionId in filterCtx }
        val byLocation = visibleEntries.groupBy { it.location.id }
        val pins = visibleEntries
            .mapNotNull { entry ->
                val col = collectionMap[entry.collectionId] ?: return@mapNotNull null
                val siblings = byLocation[entry.location.id].orEmpty()
                val isMulti = siblings.size > 1
                val anyReviewed = siblings.any { it.review != null }
                val reviewed = entry.review != null
                val kind = when {
                    isMulti && anyReviewed -> PinKind.Multi
                    isMulti -> PinKind.MultiUnreviewed
                    reviewed -> PinKind.Reviewed
                    else -> PinKind.Unreviewed
                }
                PinUi(
                    entryId = entry.id,
                    collectionId = entry.collectionId,
                    locationId = entry.location.id,
                    locationName = entry.location.displayName,
                    lat = entry.location.coordinates.lat,
                    lng = entry.location.coordinates.lng,
                    color = parseAppearanceColor(col.appearance.color),
                    colorHex = col.appearance.color,
                    darkColorHex = darkenHex(col.appearance.color),
                    kind = kind,
                )
            }.toImmutableList()
        DerivedBase(pins, collectionRows, collectionPicks)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DerivedBase(persistentListOf(), persistentListOf(), persistentListOf()),
    )

    // Must declare after `derivedBase` — Kotlin runs init blocks and property
    // initializers in source order, so dereferencing it earlier yields null.
    init {
        viewModelScope.launch {
            derivedBase.collect { pinController.setPins(it.pins) }
        }
        viewModelScope.launch {
            combine(_pinSheet, _latestEntries) { sheet, entries ->
                selectedLocationIdsFor(sheet, entries)
            }.collect { pinController.setSelection(it) }
        }
    }

    private fun selectedLocationIdsFor(sheet: PinSheet, entries: List<Entry>): Set<String> = when (sheet) {
        is PinSheet.None -> emptySet()
        is PinSheet.Peek -> {
            // Match the peek's location by coords; an unsaved candidate has no entry-backed
            // location, so the set stays empty and no pin darkens (the candidate diamond
            // is the only marker at that coord).
            entries.firstOrNull {
                it.location.coordinates.lat == sheet.lat &&
                    it.location.coordinates.lng == sheet.lng
            }?.location?.id?.value?.let { setOf(it) } ?: emptySet()
        }
        is PinSheet.Entry -> entries.firstOrNull { it.id == sheet.entry.entryId }
            ?.location?.id?.value?.let { setOf(it) } ?: emptySet()
    }

    private val existingHitsFlow = combine(
        _query,
        _liveViewport,
        _latestEntries,
        _latestCollections,
    ) { q, vp, entries, collections ->
        if (q.isBlank()) return@combine emptyList()
        val collectionMap = collections.associateBy { it.id }
        val matched = entries.filter { it.location.displayName.contains(q, ignoreCase = true) }
        val byLocation = matched.groupBy { it.location.id }
        byLocation.entries
            .map { (locationId, locationEntries) ->
                val first = locationEntries.first()
                val dots = locationEntries.mapNotNull { e ->
                    val col = collectionMap[e.collectionId] ?: return@mapNotNull null
                    parseAppearanceColor(col.appearance.color)
                }.toImmutableList()
                SearchHitUi.ExistingEntry(
                    displayName = first.location.displayName,
                    lat = first.location.coordinates.lat,
                    lng = first.location.coordinates.lng,
                    locationId = locationId,
                    dots = dots,
                )
            }
            .let { hits ->
                if (vp == null) hits
                else hits.sortedBy { h ->
                    val dLat = h.lat - vp.centerLat
                    val dLng = h.lng - vp.centerLng
                    dLat * dLat + dLng * dLng
                }
            }
            .take(20)
    }

    private suspend fun runProviderSearch(query: String): List<SearchHitUi.Candidate> {
        _isSearching.value = true
        val bias: LocationBias? = _liveBbox.value
            ?: _liveViewport.value?.let { LocationBias.Point(Coordinates(it.centerLat, it.centerLng)) }
        val result = providerRegistry.default().resolve(query, bias)
        _isSearching.value = false
        return when (result) {
            is ProviderResult.Ok -> result.value.map { c ->
                SearchHitUi.Candidate(
                    displayName = c.displayName,
                    lat = c.coordinates.lat,
                    lng = c.coordinates.lng,
                    sourceType = c.sourceType,
                    sourceId = c.sourceId,
                    detail = c.detail,
                    needsConfirmation = c.needsConfirmation,
                )
            }
            is ProviderResult.Failed -> emptyList()
        }
    }

    private val candidatesFlow: StateFlow<List<SearchHitUi.Candidate>> =
        providerRegistry.defaultFlow.flatMapLatest { provider ->
            if (provider.supportsTypeahead) {
                _query
                    .mapLatest { q ->
                        if (q.isBlank()) return@mapLatest emptyList()
                        delay(250)
                        runProviderSearch(q)
                    }
            } else {
                _submit
                    .filter { it.isNotBlank() }
                    .mapLatest { runProviderSearch(it) }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val searchHitsFlow = combine(existingHitsFlow, candidatesFlow, _query) { existing, candidates, q ->
        if (q.isBlank()) return@combine persistentListOf()
        val deduped = candidates.filter { c ->
            c.sourceId == null || locationsRepo.findByIdentity(c.sourceType, c.sourceId) == null
        }
        rankAndInterleave(existing, deduped, _liveViewport.value)
    }

    private fun rankAndInterleave(
        existing: List<SearchHitUi.ExistingEntry>,
        candidates: List<SearchHitUi.Candidate>,
        viewport: Viewport?,
    ): ImmutableList<SearchHitUi> {
        val all: List<SearchHitUi> = existing + candidates
        return if (viewport == null) {
            all.take(25).toImmutableList()
        } else {
            all.sortedWith(
                compareBy(
                    { h ->
                        val dLat = h.lat - viewport.centerLat
                        val dLng = h.lng - viewport.centerLng
                        dLat * dLat + dLng * dLng
                    },
                    { h -> if (h is SearchHitUi.Candidate) 1 else 0 },
                )
            ).take(25).toImmutableList()
        }
    }

    private val searchThisAreaChipFlow = combine(_liveViewport, _searchContext) { vp, ctx ->
        if (ctx == null || vp == null) false
        else viewportHasDrifted(vp, ctx.viewportAtQuery)
    }

    val uiState: StateFlow<MapOverviewUiState> = combine(
        combine(
            combine(
                derivedBase,
                searchHitsFlow,
                _isSearching,
                _pinSheet,
                _error,
            ) { base, hits, searching, sheet, error ->
                MapOverviewUiState(
                    pins = base.pins,
                    collectionRows = base.collectionRows,
                    collectionPicks = base.collectionPicks,
                    searchHits = hits,
                    isSearching = searching,
                    isLoading = false,
                    pinSheet = sheet,
                    error = error,
                )
            },
            combine(_draft, _nearbyCandidates, _isResolvingNearby) { draft, nearby, resolving ->
                Triple(draft, nearby.toImmutableList(), resolving)
            },
            _pendingReview,
            _searchContext,
        ) { state, draftBundle, pendingReview, searchCtx ->
            state.copy(
                draft = draftBundle.first,
                nearbyCandidates = draftBundle.second,
                isResolvingNearby = draftBundle.third,
                pendingReview = pendingReview,
                searchContext = searchCtx,
            )
        },
        _restoredViewport,
        searchThisAreaChipFlow,
        combine(_filterContext, _collectionListSheet, _isSearchingArea) { fc, cls, searching ->
            Triple(fc, cls, searching)
        },
    ) { state, viewport, showChip, filterState ->
        state.copy(
            viewport = viewport,
            showSearchThisAreaChip = showChip,
            filterContext = filterState.first,
            collectionListSheet = filterState.second,
            isSearchingArea = filterState.third,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapOverviewUiState())

    fun openCollectionList(preselection: Set<CollectionId> = emptySet()) {
        _collectionListSheet.value = CollectionListSheet.Open(preselection)
    }

    fun closeCollectionList() {
        _collectionListSheet.value = CollectionListSheet.Closed
    }

    fun applyFilter(ids: Set<CollectionId>) {
        _filterContext.value = ids
        _collectionListSheet.value = CollectionListSheet.Closed
        fitViewportToCollections(ids)
    }

    fun clearFilter() {
        _filterContext.value = null
    }

    private fun fitViewportToCollections(ids: Set<CollectionId>) {
        val entries = _latestEntries.value.filter { it.collectionId in ids }
        if (entries.isEmpty()) return
        val lats = entries.map { it.location.coordinates.lat }
        val lngs = entries.map { it.location.coordinates.lng }
        val minLat = lats.min()
        val maxLat = lats.max()
        val minLng = lngs.min()
        val maxLng = lngs.max()
        val centerLat = (minLat + maxLat) / 2.0
        val centerLng = (minLng + maxLng) / 2.0
        val latSpan = (maxLat - minLat).coerceAtLeast(1e-4)
        val lngSpan = (maxLng - minLng).coerceAtLeast(1e-4)
        val zoomLng = ln(360.0 / (lngSpan * 1.2)) / ln(2.0)
        val zoomLat = ln(170.0 / (latSpan * 1.2)) / ln(2.0)
        val zoom = min(zoomLng, zoomLat).coerceIn(2.0, 17.0)
        _jumpToViewport.tryEmit(Viewport(centerLat = centerLat, centerLng = centerLng, zoom = zoom, bearing = 0.0))
    }

    fun commitSearchToMap() {
        val query = _query.value
        val candidates = candidatesFlow.value
        val viewport = _liveViewport.value ?: return
        if (query.isBlank() || candidates.isEmpty()) return
        _searchContext.value = SearchContext(
            query = query,
            candidates = candidates.toImmutableList(),
            viewportAtQuery = viewport,
        )
    }

    fun searchThisArea() {
        if (_isSearchingArea.value) return
        val ctx = _searchContext.value ?: return
        val viewport = _liveViewport.value ?: return
        _isSearchingArea.value = true
        viewModelScope.launch {
            try {
                val newCandidates = runProviderSearch(ctx.query)
                _searchContext.value = SearchContext(
                    query = ctx.query,
                    candidates = newCandidates.toImmutableList(),
                    viewportAtQuery = _liveViewport.value ?: viewport,
                )
            } finally {
                _isSearchingArea.value = false
            }
        }
    }

    fun clearSearch() {
        _searchContext.value = null
    }

    fun onQueryChange(value: String) {
        if (_searchContext.value != null && value != "") {
            clearSearch()
        }
        _query.value = value
    }

    fun onSubmitSearch(value: String) {
        _submit.tryEmit(value)
    }

    fun submitSearch() {
        val q = _query.value
        if (q.isBlank()) return
        _submit.tryEmit(q)
    }

    fun pickExistingHit(locationId: LocationId) {
        val entry = _latestEntries.value.firstOrNull { it.location.id == locationId } ?: return
        selectPin(entry.id)
    }

    private fun buildLocationEntries(locationId: LocationId): List<EntrySummaryUi>? {
        val entries = _latestEntries.value
        val locationEntries = entries.filter { it.location.id == locationId }
        if (locationEntries.isEmpty()) return null
        val collectionMap = _latestCollections.value.associateBy { it.id }
        val templateMap = _latestTemplates.value.associateBy { it.collectionId }
        return locationEntries.mapNotNull { e ->
            val col = collectionMap[e.collectionId] ?: return@mapNotNull null
            val template = templateMap[e.collectionId]
            val reviewedDate = if (e.review != null) formatShortDate(e.review.created) else null
            val summaryPreview = run {
                val field = template?.summaryField ?: return@run null
                val review = e.review ?: return@run null
                val raw = review.data[field]?.trim()?.takeIf { it.isNotBlank() } ?: return@run null
                val firstLine = raw.split('\n')[0]
                if (firstLine.length > 80) firstLine.take(80) + "…" else firstLine
            }
            EntrySummaryUi(
                entryId = e.id,
                collectionId = e.collectionId,
                collectionName = col.name,
                collectionColor = parseAppearanceColor(col.appearance.color),
                locationName = e.location.displayName,
                locationAddress = e.location.address,
                reviewedDate = reviewedDate,
                summaryPreview = summaryPreview,
            )
        }
    }

    fun selectPin(entryId: EntryId) {
        val entry = _latestEntries.value.find { it.id == entryId } ?: run {
            _pinSheet.value = PinSheet.None
            return
        }
        val summaries = buildLocationEntries(entry.location.id) ?: run {
            _pinSheet.value = PinSheet.None
            return
        }
        _pinSheet.value = if (summaries.size == 1) {
            PinSheet.Entry(summaries.first())
        } else {
            PinSheet.Peek(
                locationName = entry.location.displayName,
                lat = entry.location.coordinates.lat,
                lng = entry.location.coordinates.lng,
                entries = summaries.toImmutableList(),
            )
        }
    }

    fun openEntryFromPeek(entryId: EntryId) {
        val peek = _pinSheet.value as? PinSheet.Peek ?: return
        val summary = peek.entries.find { it.entryId == entryId } ?: return
        _pinSheet.value = PinSheet.Entry(summary)
    }

    fun peekLocationFromEntry() {
        val current = _pinSheet.value as? PinSheet.Entry ?: return
        val entry = _latestEntries.value.find { it.id == current.entry.entryId } ?: return
        val summaries = buildLocationEntries(entry.location.id) ?: return
        _pinSheet.value = PinSheet.Peek(
            locationName = entry.location.displayName,
            lat = entry.location.coordinates.lat,
            lng = entry.location.coordinates.lng,
            entries = summaries.toImmutableList(),
        )
    }

    fun addEntryAtPeekLocation() {
        val peek = _pinSheet.value as? PinSheet.Peek ?: return
        val location = peek.candidateLocation
            ?: peek.entries.firstOrNull()?.entryId
                ?.let { id -> _latestEntries.value.find { it.id == id }?.location }
            ?: return
        _draft.value = LocationDraftSheet.Open(
            lat = location.coordinates.lat,
            lng = location.coordinates.lng,
            displayName = location.displayName,
            phase = DraftPhase.AddToCollection,
            existingLocation = location,
        )
        _pinSheet.value = PinSheet.None
    }

    fun dismissSheet() {
        _pinSheet.value = PinSheet.None
    }

    fun startDraft(lat: Double, lng: Double, displayName: String? = null) {
        _draft.value = LocationDraftSheet.Open(lat, lng, displayName)
        findNearby()
    }

    fun pickSearchCandidate(hit: SearchHitUi.Candidate, inAddMode: Boolean, collectionId: CollectionId? = null) {
        if (hit.needsConfirmation) {
            viewModelScope.launch {
                _isSearching.value = true
                val stub = LocationCandidate(
                    coordinates = Coordinates(hit.lat, hit.lng),
                    displayName = hit.displayName,
                    sourceType = hit.sourceType,
                    sourceId = hit.sourceId,
                    cachedMetadata = null,
                    detail = hit.detail,
                    needsConfirmation = true,
                )
                val confirmed = providerRegistry.default().confirm(stub)
                _isSearching.value = false
                when (confirmed) {
                    is ProviderResult.Ok -> {
                        val c = confirmed.value
                        val resolvedHit = hit.copy(
                            lat = c.coordinates.lat,
                            lng = c.coordinates.lng,
                            displayName = c.displayName,
                            needsConfirmation = false,
                        )
                        finalizePick(resolvedHit, c.cachedMetadata, inAddMode, collectionId)
                    }
                    is ProviderResult.Failed -> _error.value = confirmed.error.name
                }
            }
            return
        }
        finalizePick(hit, cachedMetadata = null, inAddMode = inAddMode, collectionId = collectionId)
    }

    private fun finalizePick(hit: SearchHitUi.Candidate, cachedMetadata: String?, inAddMode: Boolean, collectionId: CollectionId? = null) {
        val resolved = Location(
            id = LocationId(Random.nextInt(0x1000000, 0x7fffffff).toString(16)),
            coordinates = Coordinates(hit.lat, hit.lng),
            displayName = hit.displayName,
            sourceType = hit.sourceType,
            sourceId = hit.sourceId ?: "",
            address = hit.detail,
            cachedMetadata = cachedMetadata,
            refreshable = hit.sourceId != null,
        )
        if (inAddMode) {
            val collectionPick = collectionId?.let { id ->
                derivedBase.value.collectionPicks.find { it.id == id }
            }
            _pinSheet.value = PinSheet.Peek(
                locationName = resolved.displayName,
                lat = resolved.coordinates.lat,
                lng = resolved.coordinates.lng,
                entries = persistentListOf(),
                candidateLocation = resolved,
                addToCollection = collectionPick,
                detail = hit.detail,
            )
        } else {
            _pinSheet.value = PinSheet.Peek(
                locationName = resolved.displayName,
                lat = resolved.coordinates.lat,
                lng = resolved.coordinates.lng,
                entries = persistentListOf(),
                candidateLocation = resolved,
                detail = hit.detail,
            )
        }
    }

    fun dismissDraft() {
        _draft.value = LocationDraftSheet.None
        _nearbyCandidates.value = emptyList()
        _isResolvingNearby.value = false
    }

    fun findNearby() {
        val open = _draft.value as? LocationDraftSheet.Open ?: return
        viewModelScope.launch {
            _isResolvingNearby.value = true
            val result = providerRegistry.default().resolveNearby(Coordinates(open.lat, open.lng))
            _nearbyCandidates.value = when (result) {
                is ProviderResult.Ok -> result.value.map { c ->
                    NearbyCandidateUi(
                        displayName = c.displayName,
                        lat = c.coordinates.lat,
                        lng = c.coordinates.lng,
                        detail = c.detail,
                    )
                }
                is ProviderResult.Failed -> emptyList()
            }
            _isResolvingNearby.value = false
        }
    }

    fun adoptCandidate(name: String) {
        val current = _draft.value as? LocationDraftSheet.Open ?: return
        _draft.value = current.copy(adoptedCandidate = name)
    }

    fun keepCoordinates() {
        val current = _draft.value as? LocationDraftSheet.Open ?: return
        _draft.value = current.copy(adoptedCandidate = null)
    }

    fun setManualName(name: String) {
        val current = _draft.value as? LocationDraftSheet.Open ?: return
        _draft.value = current.copy(manualName = name)
    }

    fun openAddToCollection() {
        val current = _draft.value as? LocationDraftSheet.Open ?: return
        _draft.value = current.copy(phase = DraftPhase.AddToCollection)
    }

    fun backToDraft() {
        val current = _draft.value as? LocationDraftSheet.Open ?: return
        val existing = current.existingLocation
        val summaries = existing?.let { buildLocationEntries(it.id) }
        if (existing != null && summaries != null) {
            _draft.value = LocationDraftSheet.None
            _pinSheet.value = PinSheet.Peek(
                locationName = existing.displayName,
                lat = existing.coordinates.lat,
                lng = existing.coordinates.lng,
                entries = summaries.toImmutableList(),
            )
        } else if (existing != null) {
            dismissDraft()
        } else {
            _draft.value = current.copy(phase = DraftPhase.Draft)
        }
    }

    fun pickCollectionForDraft(collectionId: CollectionId) {
        val draft = _draft.value as? LocationDraftSheet.Open ?: return
        val location = draft.existingLocation ?: Location(
            id = LocationId(Random.nextInt(0x1000000, 0x7fffffff).toString(16)),
            coordinates = Coordinates(draft.lat, draft.lng),
            displayName = draft.adoptedCandidate
                ?: draft.manualName.takeIf { it.isNotBlank() }
                ?: draft.displayName
                ?: "Unknown location",
            sourceType = SourceType.Manual,
            sourceId = "",
            address = null,
            cachedMetadata = null,
            refreshable = false,
        )
        val locationName = location.displayName
        viewModelScope.launch {
            val result = collectionsRepo.addEntry(
                collectionId = collectionId,
                location = location,
                review = ReviewDraft(persistentMapOf()),
            )
            result
                .onSuccess { entry ->
                    _pendingReview.value = PendingReviewPrompt.Pending(
                        entryId = entry.id,
                        locationName = locationName,
                    )
                    dismissDraft()
                }
                .onFailure { _error.value = "Add to Collection failed: ${it.message ?: it::class.simpleName}" }
        }
    }

    fun confirmAddCandidateAtPeek() {
        val peek = _pinSheet.value as? PinSheet.Peek ?: return
        val candidate = peek.candidateLocation ?: return
        val target = peek.addToCollection ?: return
        val locationName = candidate.displayName
        viewModelScope.launch {
            val result = collectionsRepo.addEntry(
                collectionId = target.id,
                location = candidate,
                review = ReviewDraft(persistentMapOf()),
            )
            result
                .onSuccess { entry ->
                    _pendingReview.value = PendingReviewPrompt.Pending(
                        entryId = entry.id,
                        locationName = locationName,
                    )
                    _pinSheet.value = PinSheet.None
                }
                .onFailure { _error.value = "Add to Collection failed: ${it.message ?: it::class.simpleName}" }
        }
    }

    fun clearPendingReview() {
        _pendingReview.value = PendingReviewPrompt.None
    }

    fun onVisibleBoundsChange(south: Double, west: Double, north: Double, east: Double) {
        _liveBbox.value = LocationBias.Box(south = south, west = west, north = north, east = east)
    }

    fun onViewportChange(viewport: Viewport) {
        _liveViewport.value = viewport
        viewModelScope.launch { session.saveViewport(viewport) }
    }

    private fun viewportHasDrifted(current: Viewport, atQuery: Viewport): Boolean {
        if (abs(current.zoom - atQuery.zoom) >= 1.0) return true
        val lngSpan = 360.0 / 2.0.pow(atQuery.zoom.coerceIn(0.0, 22.0))
        val threshold = lngSpan * 0.3
        return abs(current.centerLat - atQuery.centerLat) > threshold ||
            abs(current.centerLng - atQuery.centerLng) > threshold
    }
}
