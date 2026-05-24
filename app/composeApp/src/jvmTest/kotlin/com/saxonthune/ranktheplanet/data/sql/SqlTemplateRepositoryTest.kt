package com.saxonthune.ranktheplanet.data.sql

import com.saxonthune.ranktheplanet.data.db.createDatabase
import com.saxonthune.ranktheplanet.data.db.createDriver
import com.saxonthune.ranktheplanet.data.op.OpLogWriter
import com.saxonthune.ranktheplanet.domain.Appearance
import com.saxonthune.ranktheplanet.domain.FieldType
import com.saxonthune.ranktheplanet.domain.TemplateField
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlTemplateRepositoryTest {

    private fun setup(): Pair<SqlCollectionRepository, SqlTemplateRepository> {
        val db = createDatabase(createDriver("ignored"))
        val writer = OpLogWriter(db, "device-test")
        val locationRepo = SqlLocationRepository(db, writer)
        val collRepo = SqlCollectionRepository(db, writer, locationRepo)
        val templateRepo = SqlTemplateRepository(db, writer)
        return collRepo to templateRepo
    }

    @Test
    fun defineAndObserveRoundTrip() = runBlocking {
        val (collRepo, templateRepo) = setup()
        val col = collRepo.create("Col", null, Appearance("#FF0000", "dot")).getOrThrow()

        val result = templateRepo.define(col.id, listOf(
            TemplateField("rating", "Rating", FieldType.Score),
        ))
        assertTrue(result.isSuccess)

        val template = templateRepo.observe(col.id).first()
        assertNotNull(template)
        assertEquals(1, template.version)
        assertEquals(1, template.fields.size)
        assertEquals("rating", template.fields[0].name)
    }

    @Test
    fun editBumpsVersionAndOldVersionRowsRemain() = runBlocking {
        val db = createDatabase(createDriver("ignored"))
        val writer = OpLogWriter(db, "device-test")
        val locationRepo = SqlLocationRepository(db, writer)
        val collRepo = SqlCollectionRepository(db, writer, locationRepo)
        val templateRepo = SqlTemplateRepository(db, writer)

        val col = collRepo.create("Col", null, Appearance("#FF0000", "dot")).getOrThrow()
        templateRepo.define(col.id, listOf(TemplateField("notes", type = FieldType.Text))).getOrThrow()

        val editResult = templateRepo.edit(col.id, listOf(
            TemplateField("notes", type = FieldType.Text),
            TemplateField("rating", type = FieldType.Score),
        ))
        assertTrue(editResult.isSuccess)
        assertEquals(2, editResult.getOrThrow().version)

        val v1Rows = db.templateFieldQueries
            .selectByCollectionAndVersion(col.id.value, 1L)
            .executeAsList()
        assertEquals(1, v1Rows.size)

        val v2Rows = db.templateFieldQueries
            .selectByCollectionAndVersion(col.id.value, 2L)
            .executeAsList()
        assertEquals(2, v2Rows.size)
    }
}
