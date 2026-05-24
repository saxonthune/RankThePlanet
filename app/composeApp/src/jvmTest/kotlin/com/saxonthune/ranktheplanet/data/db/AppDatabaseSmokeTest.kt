package com.saxonthune.ranktheplanet.data.db

import kotlin.test.Test
import kotlin.test.assertEquals

class AppDatabaseSmokeTest {

    @Test
    fun schemaCreatesAndAllTablesAcceptRows() {
        val driver = createDriver("ignored")
        val db = createDatabase(driver)

        // Insert one row into each table and verify readback.

        driver.execute(null, """
            INSERT INTO location (id, source_type, source_id, lat, lng, display_name, refreshable)
            VALUES ('loc-1', 'manual', 'manual-1', 1.0, 2.0, 'Test Place', 0)
        """.trimIndent(), 0)

        driver.execute(null, """
            INSERT INTO collection (id, name, appearance_color, appearance_pin_style, created, last_modified)
            VALUES ('col-1', 'My Collection', '#FF0000', 'dot', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z')
        """.trimIndent(), 0)

        driver.execute(null, """
            INSERT INTO template_field (collection_id, version, ordinal, name, label, type, required)
            VALUES ('col-1', 1, 0, 'rating', 'Rating', 'score', 0)
        """.trimIndent(), 0)

        driver.execute(null, """
            INSERT INTO entry (id, collection_id, location_id, created, last_modified)
            VALUES ('entry-1', 'col-1', 'loc-1', '2024-01-01T00:00:00Z', '2024-01-01T00:00:00Z')
        """.trimIndent(), 0)

        driver.execute(null, """
            INSERT INTO op_log (op_id, ts, device_id, kind, payload)
            VALUES ('op-1', '2024-01-01T00:00:00Z', 'device-1', 'entry.submit', '{}')
        """.trimIndent(), 0)

        val locations = db.locationQueries.selectAll().executeAsList()
        assertEquals(1, locations.size)

        val collections = db.collectionQueries.selectAll().executeAsList()
        assertEquals(1, collections.size)

        val templateFields = db.templateFieldQueries.selectAll().executeAsList()
        assertEquals(1, templateFields.size)

        val entries = db.entryQueries.selectAll().executeAsList()
        assertEquals(1, entries.size)

        val opLogs = db.opLogQueries.selectAll().executeAsList()
        assertEquals(1, opLogs.size)

        driver.close()
    }
}
