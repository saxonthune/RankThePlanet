package com.saxonthune.ranktheplanet.data.sql

import com.saxonthune.ranktheplanet.data.db.createDatabase
import com.saxonthune.ranktheplanet.data.db.createDriver
import com.saxonthune.ranktheplanet.data.op.OpLogWriter
import com.saxonthune.ranktheplanet.domain.Appearance
import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.Location
import com.saxonthune.ranktheplanet.domain.LocationId
import com.saxonthune.ranktheplanet.domain.ReviewDraft
import com.saxonthune.ranktheplanet.domain.SourceType
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SqlCollectionRepositoryTest {

    private fun setup(): Triple<SqlCollectionRepository, SqlLocationRepository, com.saxonthune.ranktheplanet.db.AppDatabase> {
        val db = createDatabase(createDriver("ignored"))
        val writer = OpLogWriter(db, "device-test")
        val locationRepo = SqlLocationRepository(db, writer)
        val collRepo = SqlCollectionRepository(db, writer, locationRepo)
        return Triple(collRepo, locationRepo, db)
    }

    @Test
    fun createRoundTrips() = runBlocking {
        val (collRepo, _, _) = setup()
        val result = collRepo.create("My Collection", "A description", Appearance("#FF0000", "dot"))
        assertTrue(result.isSuccess)
        val created = result.getOrThrow()
        assertEquals("My Collection", created.name)
        assertEquals("A description", created.description)

        val fromDb = collRepo.observeAll().first()
        assertEquals(1, fromDb.size)
        assertEquals("My Collection", fromDb[0].name)
    }

    @Test
    fun addEntryWritesEntryAndBumpsLastModifiedAndAppendsOpLog() = runBlocking {
        val db = createDatabase(createDriver("ignored"))
        val writer = OpLogWriter(db, "device-test")
        val locationRepo = SqlLocationRepository(db, writer)
        val collRepo = SqlCollectionRepository(db, writer, locationRepo)

        val col = collRepo.create("Col", null, Appearance("#FF0000", "dot")).getOrThrow()
        val loc = Location(
            id = LocationId(""),
            coordinates = Coordinates(1.0, 2.0),
            displayName = "Place",
            sourceType = SourceType.Manual,
            sourceId = "manual-1",
            address = null,
            cachedMetadata = null,
            refreshable = false,
        )
        val entry = collRepo.addEntry(col.id, loc, ReviewDraft(persistentMapOf())).getOrThrow()
        assertNotNull(entry)

        val cols = collRepo.observeAll().first()
        assertEquals(1, cols.size)
        assert(cols[0].lastModified >= col.lastModified)

        val opLogs = db.opLogQueries.selectAll().executeAsList()
        assertTrue(opLogs.any { it.kind == "entry.added" })
    }
}
