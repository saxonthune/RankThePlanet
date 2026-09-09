package com.saxonthune.ranktheplanet.data.op

import com.saxonthune.ranktheplanet.data.db.createDatabase
import com.saxonthune.ranktheplanet.data.db.createDriver
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class OpLogWriterTest {

    @Test
    fun appendRoundTripsForEntryAdded() = runBlocking {
        val db = createDatabase(createDriver("ignored"))
        val writer = OpLogWriter(db, "device-test")

        writer.append(Op.EntryAdded(
            entryId = "entry-1",
            collectionId = "col-1",
            locationId = "loc-1",
            data = null,
            templateVersion = null,
        ))

        val rows = db.opLogQueries.selectAll().executeAsList()
        assertEquals(1, rows.size)
        assertEquals("entry.added", rows[0].kind)
        assertEquals("device-test", rows[0].device_id)
    }

    @Test
    fun appendRoundTripsForCollectionCreated() = runBlocking {
        val db = createDatabase(createDriver("ignored"))
        val writer = OpLogWriter(db, "device-test")

        writer.append(Op.CollectionCreated(
            collectionId = "col-1",
            name = "My Collection",
            description = null,
            appearanceColor = "#FF0000",
            appearancePinStyle = "dot",
        ))

        val rows = db.opLogQueries.selectAll().executeAsList()
        assertEquals(1, rows.size)
        assertEquals("collection.created", rows[0].kind)
    }
}
