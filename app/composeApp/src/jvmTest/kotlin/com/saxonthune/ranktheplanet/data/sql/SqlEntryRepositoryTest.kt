package com.saxonthune.ranktheplanet.data.sql

import com.saxonthune.ranktheplanet.data.db.createDatabase
import com.saxonthune.ranktheplanet.data.db.createDriver
import com.saxonthune.ranktheplanet.data.op.OpLogWriter
import com.saxonthune.ranktheplanet.domain.Appearance
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.FieldType
import com.saxonthune.ranktheplanet.domain.Location
import com.saxonthune.ranktheplanet.domain.LocationId
import com.saxonthune.ranktheplanet.domain.ReviewDraft
import com.saxonthune.ranktheplanet.domain.SourceType
import com.saxonthune.ranktheplanet.domain.TemplateField
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlEntryRepositoryTest {

    private fun setup(): Triple<SqlCollectionRepository, SqlEntryRepository, SqlTemplateRepository> {
        val db = createDatabase(createDriver("ignored"))
        val writer = OpLogWriter(db, "device-test")
        val locationRepo = SqlLocationRepository(db, writer)
        val collRepo = SqlCollectionRepository(db, writer, locationRepo)
        val entryRepo = SqlEntryRepository(db, writer)
        val templateRepo = SqlTemplateRepository(db, writer)
        return Triple(collRepo, entryRepo, templateRepo)
    }

    private fun testLocation(sourceId: String = "manual-1") = Location(
        id = LocationId(""),
        coordinates = Coordinates(1.0, 2.0),
        displayName = "Place",
        sourceType = SourceType.Manual,
        sourceId = sourceId,
        address = null,
        cachedMetadata = null,
        refreshable = false,
    )

    @Test
    fun editReviewRoundTrips() = runBlocking {
        val (collRepo, entryRepo, templateRepo) = setup()
        val col = collRepo.create("Col", null, Appearance("#FF0000", "dot")).getOrThrow()
        templateRepo.define(col.id, listOf(TemplateField("notes", type = FieldType.Text))).getOrThrow()

        val updatedCol = collRepo.observeAll().first().first { it.id == col.id }
        val entry = collRepo.addEntry(
            col.id, testLocation(),
            ReviewDraft(persistentMapOf(), updatedCol.templateVersion)
        ).getOrThrow()

        val editResult = entryRepo.editReview(
            entry.id,
            mapOf("notes" to "Great place"),
        )
        assertTrue(editResult.isSuccess)
        val updated = editResult.getOrThrow()
        assertEquals("Great place", updated.review?.data?.get("notes"))
    }

    @Test
    fun editReviewDropsKeysAbsentAtCurrentTemplateVersion() = runBlocking {
        val (collRepo, entryRepo, templateRepo) = setup()
        val col = collRepo.create("Col", null, Appearance("#FF0000", "dot")).getOrThrow()
        templateRepo.define(col.id, listOf(
            TemplateField("notes", type = FieldType.Text),
            TemplateField("oldField", type = FieldType.Text),
        )).getOrThrow()
        val v1Col = collRepo.observeAll().first().first { it.id == col.id }

        val entry = collRepo.addEntry(
            col.id, testLocation(),
            ReviewDraft(persistentMapOf("notes" to "ok", "oldField" to "stale"), v1Col.templateVersion)
        ).getOrThrow()

        templateRepo.edit(col.id, listOf(
            TemplateField("notes", type = FieldType.Text),
            TemplateField("rating", type = FieldType.Score),
        )).getOrThrow()
        val v2Col = collRepo.observeAll().first().first { it.id == col.id }

        val result = entryRepo.editReview(entry.id, mapOf("notes" to "new", "oldField" to "stale"))
        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals("new", updated.review?.data?.get("notes"))
        assertNull(updated.review?.data?.get("oldField"))
        assertEquals(v2Col.templateVersion, updated.review?.recordedTemplateVersion)
    }
}
