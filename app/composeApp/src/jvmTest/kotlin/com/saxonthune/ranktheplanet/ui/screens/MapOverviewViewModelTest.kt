package com.saxonthune.ranktheplanet.ui.screens

import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.LocationRepository
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.data.location.LocationCandidate
import com.saxonthune.ranktheplanet.data.location.LocationProvider
import com.saxonthune.ranktheplanet.data.location.LocationProviderRegistry
import com.saxonthune.ranktheplanet.data.location.ProviderResult
import com.saxonthune.ranktheplanet.data.projection.OverviewProjection
import com.saxonthune.ranktheplanet.data.session.SessionState
import com.saxonthune.ranktheplanet.data.session.SessionStateStore
import com.saxonthune.ranktheplanet.domain.Appearance
import com.saxonthune.ranktheplanet.domain.Collection
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.Entry
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.Location
import com.saxonthune.ranktheplanet.domain.LocationId
import com.saxonthune.ranktheplanet.domain.MapOverviewState
import com.saxonthune.ranktheplanet.domain.ReviewDraft
import com.saxonthune.ranktheplanet.domain.ReviewTemplate
import com.saxonthune.ranktheplanet.domain.SourceType
import com.saxonthune.ranktheplanet.domain.TemplateField
import com.saxonthune.ranktheplanet.domain.Viewport
import com.saxonthune.ranktheplanet.domain.VisiblePin
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MapOverviewViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val testViewport = Viewport(40.0, -74.0, 12.0, 0.0)

    private val coffeeCandidate = LocationCandidate(
        coordinates = Coordinates(40.01, -74.01),
        displayName = "Coffee Place",
        sourceType = SourceType.Fake,
        sourceId = "fake-1",
        cachedMetadata = null,
        detail = "123 Test St",
    )

    private fun makeVm(
        candidates: List<LocationCandidate> = listOf(coffeeCandidate),
        supportsTypeahead: Boolean = false,
    ): MapOverviewViewModel {
        val provider = object : LocationProvider {
            override val type = SourceType.Fake
            override val supportsTypeahead = supportsTypeahead
            override suspend fun resolve(query: String, near: Coordinates?) =
                ProviderResult.Ok(if (query.isBlank()) emptyList() else candidates)
            override suspend fun resolveNearby(coordinates: Coordinates) =
                ProviderResult.Ok(emptyList<LocationCandidate>())
            override suspend fun healthCheck() = ProviderResult.Ok(Unit)
        }

        val registry = object : LocationProviderRegistry {
            private val _flow = MutableStateFlow<LocationProvider>(provider)
            override val defaultFlow: StateFlow<LocationProvider> = _flow
            override fun default() = provider
            override fun providerFor(type: SourceType) = provider.takeIf { it.type == type }
            override fun configured() = listOf(provider)
            override fun setDefault(type: SourceType) {}
            override fun addProvider(p: LocationProvider) {}
            override suspend fun saveProviderKey(type: SourceType, key: String) {}
        }

        val collectionRepo = object : CollectionRepository {
            override fun observeAll(): Flow<List<Collection>> = MutableStateFlow(emptyList())
            override fun observe(id: CollectionId): Flow<Collection?> = MutableStateFlow(null)
            override suspend fun create(name: String, description: String?, appearance: Appearance, powerRanking: Boolean) =
                Result.failure<Collection>(UnsupportedOperationException())
            override suspend fun editMetadata(id: CollectionId, name: String, description: String?, appearance: Appearance, powerRanking: Boolean) =
                Result.failure<Collection>(UnsupportedOperationException())
            override suspend fun addEntry(collectionId: CollectionId, location: Location, review: ReviewDraft) =
                Result.failure<Entry>(UnsupportedOperationException())
            override suspend fun removeEntry(entryId: EntryId) =
                Result.failure<Unit>(UnsupportedOperationException())
        }

        val entryRepo = object : EntryRepository {
            override fun observeAll(): Flow<List<Entry>> = MutableStateFlow(emptyList())
            override fun observeByCollection(collectionId: CollectionId): Flow<List<Entry>> = MutableStateFlow(emptyList())
            override fun observe(entryId: EntryId): Flow<Entry?> = MutableStateFlow(null)
            override suspend fun editReview(entryId: EntryId, data: Map<String, String>, templateVersion: Int) =
                Result.failure<Entry>(UnsupportedOperationException())
        }

        val locationRepo = object : LocationRepository {
            override suspend fun findByIdentity(sourceType: SourceType, sourceId: String): Location? = null
            override suspend fun upsert(location: Location) = Result.failure<Location>(UnsupportedOperationException())
            override suspend fun merge(keep: LocationId, drop: LocationId) = Result.failure<Location>(UnsupportedOperationException())
        }

        val templateRepo = object : TemplateRepository {
            override fun observe(collectionId: CollectionId): Flow<ReviewTemplate?> = MutableStateFlow(null)
            override suspend fun define(collectionId: CollectionId, fields: List<TemplateField>) =
                Result.failure<ReviewTemplate>(UnsupportedOperationException())
            override suspend fun edit(collectionId: CollectionId, fields: List<TemplateField>) =
                Result.failure<ReviewTemplate>(UnsupportedOperationException())
        }

        val projection = object : OverviewProjection {
            override suspend fun loadOverview() = MapOverviewState(
                viewport = testViewport,
                visiblePins = persistentListOf(),
                collectionFilter = persistentSetOf(),
            )
            override fun observeOverview() = flowOf(
                MapOverviewState(
                    viewport = testViewport,
                    visiblePins = persistentListOf(),
                    collectionFilter = persistentSetOf(),
                )
            )
        }

        val session = object : SessionStateStore {
            override suspend fun load() = SessionState()
            override fun observeViewport() = flowOf(testViewport)
            override suspend fun saveViewport(viewport: Viewport) {}
            override suspend fun saveSelectedPin(entryId: EntryId?) {}
        }

        return MapOverviewViewModel(collectionRepo, entryRepo, registry, templateRepo, projection, session, locationRepo)
    }

    @Test
    fun commitSearchToMap_setsSearchContextAndClearsQuery() = runTest {
        val vm = makeVm()
        val collector = launch { vm.uiState.collect {} }

        vm.onViewportChange(testViewport)
        vm.onQueryChange("coffee")
        vm.onSubmitSearch("coffee")
        advanceUntilIdle()

        vm.commitSearchToMap()

        val state = vm.uiState.value
        assertNotNull(state.searchContext)
        assertEquals("coffee", state.searchContext!!.query)
        assertEquals(1, state.searchContext!!.candidates.size)
        assertEquals("Coffee Place", state.searchContext!!.candidates[0].displayName)
        assertTrue(state.isSearchResultsMode)
        assertTrue(state.searchHits.isEmpty())

        collector.cancel()
    }

    @Test
    fun commitSearchToMap_noop_whenNoCandidates() = runTest {
        val vm = makeVm(candidates = emptyList())
        val collector = launch { vm.uiState.collect {} }

        vm.onViewportChange(testViewport)
        vm.onQueryChange("coffee")
        vm.onSubmitSearch("coffee")
        advanceUntilIdle()

        vm.commitSearchToMap()

        assertNull(vm.uiState.value.searchContext)

        collector.cancel()
    }

    @Test
    fun searchThisArea_refreshesCandidatesAndViewport() = runTest {
        val vm = makeVm()
        val collector = launch { vm.uiState.collect {} }

        vm.onViewportChange(testViewport)
        vm.onQueryChange("coffee")
        vm.onSubmitSearch("coffee")
        advanceUntilIdle()
        vm.commitSearchToMap()

        val newViewport = Viewport(41.0, -75.0, 14.0, 0.0)
        vm.onViewportChange(newViewport)
        vm.searchThisArea()
        advanceUntilIdle()

        val ctx = vm.uiState.value.searchContext
        assertNotNull(ctx)
        assertEquals("coffee", ctx!!.query)
        assertEquals(newViewport, ctx.viewportAtQuery)
        assertEquals(1, ctx.candidates.size)

        collector.cancel()
    }

    @Test
    fun onQueryChange_clearsSearchContext_whenSet() = runTest {
        val vm = makeVm()
        val collector = launch { vm.uiState.collect {} }

        vm.onViewportChange(testViewport)
        vm.onQueryChange("coffee")
        vm.onSubmitSearch("coffee")
        advanceUntilIdle()
        vm.commitSearchToMap()

        assertNotNull(vm.uiState.value.searchContext)

        vm.onQueryChange("c")

        assertNull(vm.uiState.value.searchContext)

        collector.cancel()
    }

    @Test
    fun clearSearch_dropsContext() = runTest {
        val vm = makeVm()
        val collector = launch { vm.uiState.collect {} }

        vm.onViewportChange(testViewport)
        vm.onQueryChange("coffee")
        vm.onSubmitSearch("coffee")
        advanceUntilIdle()
        vm.commitSearchToMap()

        assertNotNull(vm.uiState.value.searchContext)

        vm.clearSearch()

        assertNull(vm.uiState.value.searchContext)

        collector.cancel()
    }
}
