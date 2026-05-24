package com.saxonthune.ranktheplanet.data.sql

import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.LocationRepository
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.data.op.OpLogWriter
import com.saxonthune.ranktheplanet.db.AppDatabase

class SqlRepositories(database: AppDatabase, deviceId: String) {
    private val opLogWriter = OpLogWriter(database, deviceId)
    private val locationRepo = SqlLocationRepository(database, opLogWriter)
    val locations: LocationRepository = locationRepo
    val collections: CollectionRepository = SqlCollectionRepository(database, opLogWriter, locationRepo)
    val entries: EntryRepository = SqlEntryRepository(database, opLogWriter)
    val templates: TemplateRepository = SqlTemplateRepository(database, opLogWriter)
}
