package com.saxonthune.ranktheplanet.ui.screens

import com.saxonthune.ranktheplanet.data.CollectionPortIoService
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.ExportedBundle
import com.saxonthune.ranktheplanet.data.ImportProgress
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.data.db.createDatabase
import com.saxonthune.ranktheplanet.data.db.createDriver
import com.saxonthune.ranktheplanet.data.sql.SqlRepositories
import com.saxonthune.ranktheplanet.domain.Appearance
import com.saxonthune.ranktheplanet.domain.Collection
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.Entry
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.FieldType
import com.saxonthune.ranktheplanet.domain.Location
import com.saxonthune.ranktheplanet.domain.LocationId
import com.saxonthune.ranktheplanet.domain.ReviewDraft
import com.saxonthune.ranktheplanet.domain.ReviewInstance
import com.saxonthune.ranktheplanet.domain.ReviewTemplate
import com.saxonthune.ranktheplanet.domain.SourceType
import com.saxonthune.ranktheplanet.domain.TemplateField
import com.saxonthune.ranktheplanet.domain.io.PortFormat
import com.saxonthune.ranktheplanet.io.FilePicker
import com.saxonthune.ranktheplanet.io.PickedFile
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val collectionId = CollectionId("test-col")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── helpers for sort tests ────────────────────────────────────────────────

    private fun makeCollection(powerRanking: Boolean = false) = Collection(
        id = collectionId,
        name = "Test",
        appearance = Appearance("#FF0000", "dot"),
        templateVersion = 1,
        isVisible = true,
        powerRanking = powerRanking,
        created = "2024-01-01T00:00:00Z",
        lastModified = "2024-01-01T00:00:00Z",
    )

    private fun makeEntry(id: String, added: String, score: Double? = null) = Entry(
        id = EntryId(id),
        collectionId = collectionId,
        location = Location(
            id = LocationId(id),
            coordinates = Coordinates(0.0, 0.0),
            displayName = "Place $id",
            sourceType = SourceType.Manual,
            sourceId = id,
            address = null,
            cachedMetadata = null,
            refreshable = false,
        ),
        review = if (score != null) ReviewInstance(
            data = persistentMapOf("score" to score.toString()),
            recordedTemplateVersion = 1,
            created = added,
            lastModified = added,
        ) else null,
        added = added,
    )

    private fun makeTemplate(withScore: Boolean = false) = ReviewTemplate(
        collectionId = collectionId,
        version = 1,
        fields = if (withScore) persistentListOf(
            TemplateField(name = "score", type = FieldType.Score)
        ) else persistentListOf(),
    )

    private fun vm(
        collection: Collection,
        entries: List<Entry> = emptyList(),
        template: ReviewTemplate? = null,
        portIo: CollectionPortIoService = CdvFakePortIoService(),
        filePicker: FilePicker = CdvFakeFilePicker(),
    ): CollectionDetailViewModel {
        val collectionFlow = MutableStateFlow<Collection?>(collection)
        val entriesFlow = MutableStateFlow(entries)
        val templateFlow = MutableStateFlow(template)

        val collectionRepo = object : CollectionRepository {
            override fun observeAll(): Flow<List<Collection>> = MutableStateFlow(listOf(collection))
            override fun observe(id: CollectionId): Flow<Collection?> = collectionFlow
            override suspend fun create(name: String, description: String?, appearance: Appearance, powerRanking: Boolean) = Result.failure<Collection>(UnsupportedOperationException())
            override suspend fun editMetadata(id: CollectionId, name: String, description: String?, appearance: Appearance, powerRanking: Boolean) = Result.failure<Collection>(UnsupportedOperationException())
            override suspend fun addEntry(collectionId: CollectionId, location: Location, review: ReviewDraft) = Result.failure<Entry>(UnsupportedOperationException())
            override suspend fun removeEntry(entryId: EntryId) = Result.failure<Unit>(UnsupportedOperationException())
        }
        val entryRepo = object : EntryRepository {
            override fun observeAll(): Flow<List<Entry>> = MutableStateFlow(emptyList())
            override fun observeByCollection(collectionId: CollectionId): Flow<List<Entry>> = entriesFlow
            override fun observe(entryId: EntryId): Flow<Entry?> = MutableStateFlow(null)
            override suspend fun editReview(entryId: EntryId, data: Map<String, String>) = Result.failure<Entry>(UnsupportedOperationException())
        }
        val templateRepo = object : TemplateRepository {
            override fun observe(collectionId: CollectionId): Flow<ReviewTemplate?> = templateFlow
            override suspend fun define(collectionId: CollectionId, fields: List<TemplateField>) = Result.failure<ReviewTemplate>(UnsupportedOperationException())
            override suspend fun edit(collectionId: CollectionId, fields: List<TemplateField>) = Result.failure<ReviewTemplate>(UnsupportedOperationException())
        }

        return CollectionDetailViewModel(collectionId, collectionRepo, entryRepo, templateRepo, portIo, filePicker)
    }

    // ── helpers for export tests ──────────────────────────────────────────────

    private fun makeRepos(): SqlRepositories {
        val db = createDatabase(createDriver("ignored"))
        return SqlRepositories(db, "test-device")
    }

    // ── export tests ──────────────────────────────────────────────────────────

    @Test
    fun exportAs_callsServiceThenFilePicker() = runTest {
        val repos = makeRepos()
        val bundle = ExportedBundle("kml content", "test.kml", PortFormat.Kml)
        val fakeService = CdvFakePortIoService(exportResult = Result.success(bundle))
        val fakePicker = CdvFakeFilePicker()

        val vm = CollectionDetailViewModel(
            collectionId,
            repos.collections, repos.entries, repos.templates,
            fakeService, fakePicker,
        )

        vm.exportAs(PortFormat.Kml)
        advanceUntilIdle()

        assertEquals(collectionId to PortFormat.Kml, fakeService.exportCalledWith)
        assertEquals(Triple("test.kml", PortFormat.Kml, "kml content"), fakePicker.shareCalledWith)
    }

    @Test
    fun exportAs_serviceFailure_filePickerNotCalled() = runTest {
        val repos = makeRepos()
        val fakeService = CdvFakePortIoService(exportResult = Result.failure(Exception("not found")))
        val fakePicker = CdvFakeFilePicker()

        val vm = CollectionDetailViewModel(
            collectionId,
            repos.collections, repos.entries, repos.templates,
            fakeService, fakePicker,
        )

        vm.exportAs(PortFormat.Kml)
        advanceUntilIdle()

        assertNull(fakePicker.shareCalledWith)
    }

    // ── sort tests ────────────────────────────────────────────────────────────

    @Test
    fun `availableSortModes always includes DateAdded and ReviewTime`() = runTest {
        val viewModel = vm(makeCollection())
        val state = viewModel.uiState.first { !it.isLoading }
        assertTrue(SortMode.DateAdded in state.availableSortModes)
        assertTrue(SortMode.ReviewTime in state.availableSortModes)
    }

    @Test
    fun `availableSortModes includes Score iff template has score field`() = runTest {
        val vmWithScore = vm(makeCollection(), template = makeTemplate(withScore = true))
        val stateWithScore = vmWithScore.uiState.first { !it.isLoading }
        assertTrue(SortMode.Score in stateWithScore.availableSortModes)

        val vmNoScore = vm(makeCollection(), template = makeTemplate(withScore = false))
        val stateNoScore = vmNoScore.uiState.first { !it.isLoading }
        assertFalse(SortMode.Score in stateNoScore.availableSortModes)
    }

    @Test
    fun `availableSortModes includes PowerRank iff collection powerRanking is true`() = runTest {
        val vmPower = vm(makeCollection(powerRanking = true))
        val statePower = vmPower.uiState.first { !it.isLoading }
        assertTrue(SortMode.PowerRank in statePower.availableSortModes)

        val vmNoPower = vm(makeCollection(powerRanking = false))
        val stateNoPower = vmNoPower.uiState.first { !it.isLoading }
        assertFalse(SortMode.PowerRank in stateNoPower.availableSortModes)
    }

    @Test
    fun `toggleSortDirection flips direction and entries are reversed for DateAdded`() = runTest {
        val entries = listOf(
            makeEntry("a", "2024-01-01T00:00:00Z"),
            makeEntry("b", "2024-02-01T00:00:00Z"),
            makeEntry("c", "2024-03-01T00:00:00Z"),
        )
        val viewModel = vm(makeCollection(), entries = entries)

        val stateDesc = viewModel.uiState.first { !it.isLoading }
        assertEquals(SortDirection.Descending, stateDesc.sortDirection)
        assertEquals(listOf("c", "b", "a"), stateDesc.entries.map { it.id.value })

        viewModel.toggleSortDirection()
        val stateAsc = viewModel.uiState.first { it.sortDirection == SortDirection.Ascending }
        assertEquals(SortDirection.Ascending, stateAsc.sortDirection)
        assertEquals(listOf("a", "b", "c"), stateAsc.entries.map { it.id.value })
    }

    @Test
    fun `PowerRank orders by added ascending regardless of sortDirection`() = runTest {
        val entries = listOf(
            makeEntry("a", "2024-01-01T00:00:00Z"),
            makeEntry("b", "2024-02-01T00:00:00Z"),
            makeEntry("c", "2024-03-01T00:00:00Z"),
        )
        val viewModel = vm(makeCollection(powerRanking = true), entries = entries)
        val initialState = viewModel.uiState.first { !it.isLoading }

        viewModel.setSort(SortMode.PowerRank)
        val statePower = viewModel.uiState.first { it.sortMode == SortMode.PowerRank }
        assertEquals(listOf("a", "b", "c"), statePower.entries.map { it.id.value })

        viewModel.toggleSortDirection()
        val stateToggled = viewModel.uiState.first { it.sortDirection == SortDirection.Ascending }
        assertEquals(SortMode.PowerRank, stateToggled.sortMode)
        assertEquals(listOf("a", "b", "c"), stateToggled.entries.map { it.id.value })
    }
}

// ── test doubles ──────────────────────────────────────────────────────────────

private class CdvFakeFilePicker : FilePicker {
    var shareCalledWith: Triple<String, PortFormat, String>? = null

    override suspend fun openForRead(formats: List<PortFormat>): Result<PickedFile> =
        Result.failure(NotImplementedError())

    override suspend fun saveAs(suggestedName: String, format: PortFormat, text: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun share(suggestedName: String, format: PortFormat, text: String): Result<Unit> {
        shareCalledWith = Triple(suggestedName, format, text)
        return Result.success(Unit)
    }
}

private class CdvFakePortIoService(
    private val exportResult: Result<ExportedBundle>? = null,
) : CollectionPortIoService {
    var exportCalledWith: Pair<CollectionId, PortFormat>? = null

    override suspend fun export(collectionId: CollectionId, format: PortFormat): Result<ExportedBundle> {
        exportCalledWith = collectionId to format
        return exportResult ?: Result.failure(NotImplementedError())
    }

    override suspend fun import(
        text: String,
        format: PortFormat,
        onProgress: (ImportProgress) -> Unit,
    ): Result<CollectionId> =
        Result.failure(NotImplementedError())
}
