package com.saxonthune.ranktheplanet.ui.screens

import com.saxonthune.ranktheplanet.data.CollectionPortIoService
import com.saxonthune.ranktheplanet.data.ExportedBundle
import com.saxonthune.ranktheplanet.data.ImportProgress
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.io.PortFormat
import com.saxonthune.ranktheplanet.io.FilePicker
import com.saxonthune.ranktheplanet.io.PickedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ImportFlowViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun successPath_emitsImportedEvent() = runTest {
        val collectionId = CollectionId("col-1")
        val picked = PickedFile("places.geojson", "{}", PortFormat.GeoJson)
        val fakePicker = FakeFilePicker(openResult = Result.success(picked))
        val fakeService = FakePortIoService(importResult = Result.success(collectionId))

        val vm = ImportFlowViewModel(fakeService, fakePicker)
        vm.onPickFile()
        advanceUntilIdle()

        assertEquals(ImportFlowUiState.Phase.Done, vm.uiState.value.phase)
        assertNull(vm.uiState.value.error)

        val event = vm.events.first()
        assertIs<ImportFlowEvent.Imported>(event)
        assertEquals(collectionId, event.collectionId)
    }

    @Test
    fun pickerFailure_setsError_noEvent() = runTest {
        val fakePicker = FakeFilePicker(openResult = Result.failure(Exception("picker error")))
        val fakeService = FakePortIoService()

        val vm = ImportFlowViewModel(fakeService, fakePicker)
        vm.onPickFile()
        advanceUntilIdle()

        assertEquals(ImportFlowUiState.Phase.Idle, vm.uiState.value.phase)
        assertNotNull(vm.uiState.value.error)
        // service should never have been called
        assertNull(fakeService.importCalledWith)
    }

    @Test
    fun serviceFailure_setsError_noEvent() = runTest {
        val picked = PickedFile("places.kml", "<kml/>", PortFormat.Kml)
        val fakePicker = FakeFilePicker(openResult = Result.success(picked))
        val fakeService = FakePortIoService(importResult = Result.failure(Exception("bad kml")))

        val vm = ImportFlowViewModel(fakeService, fakePicker)
        vm.onPickFile()
        advanceUntilIdle()

        assertEquals(ImportFlowUiState.Phase.Idle, vm.uiState.value.phase)
        assertNotNull(vm.uiState.value.error)
    }
}

// ── test doubles ──────────────────────────────────────────────────────────────

private class FakeFilePicker(
    private val openResult: Result<PickedFile>? = null,
) : FilePicker {
    var shareCalledWith: Triple<String, PortFormat, String>? = null

    override suspend fun openForRead(formats: List<PortFormat>): Result<PickedFile> =
        openResult ?: Result.failure(NotImplementedError("not configured"))

    override suspend fun saveAs(suggestedName: String, format: PortFormat, text: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun share(suggestedName: String, format: PortFormat, text: String): Result<Unit> {
        shareCalledWith = Triple(suggestedName, format, text)
        return Result.success(Unit)
    }
}

private class FakePortIoService(
    private val importResult: Result<CollectionId>? = null,
    private val exportResult: Result<ExportedBundle>? = null,
) : CollectionPortIoService {
    var importCalledWith: Pair<String, PortFormat>? = null
    var exportCalledWith: Pair<CollectionId, PortFormat>? = null

    override suspend fun import(
        text: String,
        format: PortFormat,
        onProgress: (ImportProgress) -> Unit,
    ): Result<CollectionId> {
        importCalledWith = text to format
        return importResult ?: Result.failure(NotImplementedError("not configured"))
    }

    override suspend fun export(collectionId: CollectionId, format: PortFormat): Result<ExportedBundle> {
        exportCalledWith = collectionId to format
        return exportResult ?: Result.failure(NotImplementedError("not configured"))
    }
}
