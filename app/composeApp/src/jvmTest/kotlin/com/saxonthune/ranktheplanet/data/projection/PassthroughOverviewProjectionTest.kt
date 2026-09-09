package com.saxonthune.ranktheplanet.data.projection

import com.saxonthune.ranktheplanet.data.db.createDatabase
import com.saxonthune.ranktheplanet.data.db.createDriver
import com.saxonthune.ranktheplanet.data.op.OpLogWriter
import com.saxonthune.ranktheplanet.data.session.DEFAULT_VIEWPORT
import com.saxonthune.ranktheplanet.data.session.FileSessionStateStore
import com.saxonthune.ranktheplanet.data.sql.SqlCollectionRepository
import com.saxonthune.ranktheplanet.data.sql.SqlLocationRepository
import com.saxonthune.ranktheplanet.domain.Appearance
import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.Location
import com.saxonthune.ranktheplanet.domain.LocationId
import com.saxonthune.ranktheplanet.domain.ReviewDraft
import com.saxonthune.ranktheplanet.domain.SourceType
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PassthroughOverviewProjectionTest {

    private fun tempDir() = Files.createTempDirectory("rtp-test").toFile().absolutePath

    @Test
    fun loadOverviewReturnsEmptyWhenNoPins() = runBlocking {
        val db = createDatabase(createDriver("ignored"))
        val session = FileSessionStateStore(
            CoroutineScope(Dispatchers.Default + SupervisorJob()),
            dataDir = tempDir(),
        )
        val projection = PassthroughOverviewProjection(db, session)

        val state = projection.loadOverview()

        assertTrue(state.visiblePins.isEmpty())
        assertEquals(DEFAULT_VIEWPORT, state.viewport)
    }

    @Test
    fun loadOverviewReturnsOnlyVisibleCollectionPins() = runBlocking {
        val driver = createDriver("ignored")
        val db = createDatabase(driver)
        val writer = OpLogWriter(db, "device-test")
        val locationRepo = SqlLocationRepository(db, writer)
        val collRepo = SqlCollectionRepository(db, writer, locationRepo)

        val visible = collRepo.create("Visible", null, Appearance("#FF0000", "dot")).getOrThrow()
        val hidden = collRepo.create("Hidden", null, Appearance("#0000FF", "dot")).getOrThrow()

        // Mark the second collection as not visible
        driver.execute(null,
            "UPDATE collection SET is_visible = 0 WHERE id = '${hidden.id.value}'", 0)

        val loc1 = Location(
            id = LocationId(""),
            coordinates = Coordinates(40.0, -74.0),
            displayName = "Place A",
            sourceType = SourceType.Manual,
            sourceId = "manual-1",
            address = null,
            cachedMetadata = null,
            refreshable = false,
        )
        val loc2 = Location(
            id = LocationId(""),
            coordinates = Coordinates(41.0, -75.0),
            displayName = "Place B",
            sourceType = SourceType.Manual,
            sourceId = "manual-2",
            address = null,
            cachedMetadata = null,
            refreshable = false,
        )

        collRepo.addEntry(visible.id, loc1, ReviewDraft(persistentMapOf())).getOrThrow()
        collRepo.addEntry(hidden.id, loc2, ReviewDraft(persistentMapOf())).getOrThrow()

        val session = FileSessionStateStore(
            CoroutineScope(Dispatchers.Default + SupervisorJob()),
            dataDir = tempDir(),
        )
        val projection = PassthroughOverviewProjection(db, session)

        val state = projection.loadOverview()

        assertEquals(1, state.visiblePins.size,
            "Only pins from visible collections should appear")
        assertEquals(1, state.collectionFilter.size,
            "collectionFilter should contain only visible collection ids")
        assertEquals(visible.id, state.collectionFilter.first())
    }

    @Test
    fun observeOverviewEmitsFirstFrame() = runBlocking {
        val db = createDatabase(createDriver("ignored"))
        val session = FileSessionStateStore(
            CoroutineScope(Dispatchers.Default + SupervisorJob()),
            dataDir = tempDir(),
        )
        val projection = PassthroughOverviewProjection(db, session)

        val state = projection.observeOverview().first()

        assertTrue(state.visiblePins.isEmpty())
        assertEquals(DEFAULT_VIEWPORT, state.viewport)
    }
}
