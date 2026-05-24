package com.saxonthune.ranktheplanet.data.sql

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saxonthune.ranktheplanet.data.LocationRepository
import com.saxonthune.ranktheplanet.data.op.Op
import com.saxonthune.ranktheplanet.data.op.OpLogWriter
import com.saxonthune.ranktheplanet.data.op.generateUuid
import com.saxonthune.ranktheplanet.db.AppDatabase
import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.Location
import com.saxonthune.ranktheplanet.domain.LocationId
import com.saxonthune.ranktheplanet.domain.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class SqlLocationRepository(
    private val database: AppDatabase,
    private val opLogWriter: OpLogWriter,
) : LocationRepository {

    override suspend fun findByIdentity(sourceType: SourceType, sourceId: String): Location? =
        withContext(Dispatchers.Default) {
            database.locationQueries.findByIdentity(sourceType.name, sourceId)
                .executeAsOneOrNull()
                ?.toDomain()
        }

    override suspend fun upsert(location: Location): Result<Location> = withContext(Dispatchers.Default) {
        runCatching {
            database.transactionWithResult {
                val result = upsertInternal(location)
                opLogWriter.append(Op.LocationUpserted(
                    locationId = result.id.value,
                    sourceType = result.sourceType.name,
                    sourceId = result.sourceId,
                    lat = result.coordinates.lat,
                    lng = result.coordinates.lng,
                    displayName = result.displayName,
                ))
                result
            }
        }
    }

    override suspend fun merge(keep: LocationId, drop: LocationId): Result<Location> =
        withContext(Dispatchers.Default) {
            runCatching {
                database.transactionWithResult {
                    val keepLocation = database.locationQueries.selectById(keep.value)
                        .executeAsOneOrNull()
                        ?.toDomain()
                        ?: throw IllegalArgumentException("Location not found: ${keep.value}")
                    database.entryQueries.updateLocationId(keep.value, drop.value)
                    database.locationQueries.delete(drop.value)
                    opLogWriter.append(Op.LocationMerged(keepId = keep.value, dropId = drop.value))
                    keepLocation
                }
            }
        }

    internal fun upsertInternal(location: Location): Location {
        val existing = database.locationQueries
            .findByIdentity(location.sourceType.name, location.sourceId)
            .executeAsOneOrNull()
        return if (existing != null) {
            database.locationQueries.update(
                source_type = location.sourceType.name,
                source_id = location.sourceId,
                lat = location.coordinates.lat,
                lng = location.coordinates.lng,
                display_name = location.displayName,
                cached_metadata = location.cachedMetadata,
                refreshable = if (location.refreshable) 1L else 0L,
                id = existing.id,
            )
            location.copy(id = LocationId(existing.id))
        } else {
            val id = location.id.value.ifEmpty { generateUuid() }
            database.locationQueries.insert(
                id = id,
                source_type = location.sourceType.name,
                source_id = location.sourceId,
                lat = location.coordinates.lat,
                lng = location.coordinates.lng,
                display_name = location.displayName,
                cached_metadata = location.cachedMetadata,
                refreshable = if (location.refreshable) 1L else 0L,
            )
            location.copy(id = LocationId(id))
        }
    }
}

internal fun com.saxonthune.ranktheplanet.db.Location.toDomain() = Location(
    id = LocationId(id),
    coordinates = Coordinates(lat = lat, lng = lng),
    displayName = display_name,
    sourceType = SourceType.valueOf(source_type),
    sourceId = source_id,
    cachedMetadata = cached_metadata,
    refreshable = refreshable == 1L,
)
