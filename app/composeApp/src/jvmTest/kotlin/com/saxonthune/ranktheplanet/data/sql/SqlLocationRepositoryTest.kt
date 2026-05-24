package com.saxonthune.ranktheplanet.data.sql

import com.saxonthune.ranktheplanet.data.db.createDatabase
import com.saxonthune.ranktheplanet.data.db.createDriver
import com.saxonthune.ranktheplanet.data.op.OpLogWriter
import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.Location
import com.saxonthune.ranktheplanet.domain.LocationId
import com.saxonthune.ranktheplanet.domain.SourceType
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlLocationRepositoryTest {

    private fun setup(): Pair<SqlLocationRepository, OpLogWriter> {
        val db = createDatabase(createDriver("ignored"))
        val writer = OpLogWriter(db, "device-test")
        return SqlLocationRepository(db, writer) to writer
    }

    private fun testLocation(id: String = "loc-1") = Location(
        id = LocationId(id),
        coordinates = Coordinates(lat = 1.0, lng = 2.0),
        displayName = "Test Place",
        sourceType = SourceType.Manual,
        sourceId = "manual-test",
        cachedMetadata = null,
        refreshable = false,
    )

    @Test
    fun upsertInsertsNewLocation() = runBlocking {
        val (repo, _) = setup()
        val loc = testLocation()
        val result = repo.upsert(loc)
        assertTrue(result.isSuccess)
        val found = repo.findByIdentity(SourceType.Manual, "manual-test")
        assertNotNull(found)
        assertEquals("Test Place", found.displayName)
    }

    @Test
    fun upsertPreservesSurrogateIdOnIdentityHit() = runBlocking {
        val (repo, _) = setup()
        val loc = testLocation("original-id")
        repo.upsert(loc)

        val updatedLoc = loc.copy(id = LocationId("new-id"), displayName = "Updated")
        repo.upsert(updatedLoc)

        val found = repo.findByIdentity(SourceType.Manual, "manual-test")
        assertNotNull(found)
        assertEquals("original-id", found.id.value)
        assertEquals("Updated", found.displayName)
    }

    @Test
    fun mergeMoveEntriesAndDeletesDropped() = runBlocking {
        val db = createDatabase(createDriver("ignored"))
        val writer = OpLogWriter(db, "device-test")
        val locationRepo = SqlLocationRepository(db, writer)
        val collRepo = SqlCollectionRepository(db, writer, locationRepo)

        val col = collRepo.create("Col", null, com.saxonthune.ranktheplanet.domain.Appearance("#FF0000", "dot")).getOrThrow()

        val loc1 = Location(LocationId(""), Coordinates(1.0, 2.0), "Place A", SourceType.Manual, "manual-a", null, false)
        val loc2 = Location(LocationId(""), Coordinates(3.0, 4.0), "Place B", SourceType.Manual, "manual-b", null, false)

        val entry = collRepo.addEntry(col.id, loc1, com.saxonthune.ranktheplanet.domain.ReviewDraft(kotlinx.collections.immutable.persistentMapOf())).getOrThrow()
        val inserted1 = locationRepo.findByIdentity(SourceType.Manual, "manual-a")!!
        locationRepo.upsert(loc2)
        val inserted2 = locationRepo.findByIdentity(SourceType.Manual, "manual-b")!!

        val mergeResult = locationRepo.merge(keep = inserted1.id, drop = inserted2.id)
        assertTrue(mergeResult.isSuccess)

        assertNull(locationRepo.findByIdentity(SourceType.Manual, "manual-b"))
    }
}
