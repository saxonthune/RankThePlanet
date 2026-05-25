package com.saxonthune.ranktheplanet.data.sql

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.op.Op
import com.saxonthune.ranktheplanet.data.op.OpLogWriter
import com.saxonthune.ranktheplanet.data.op.generateUuid
import com.saxonthune.ranktheplanet.db.AppDatabase
import com.saxonthune.ranktheplanet.domain.Appearance
import com.saxonthune.ranktheplanet.domain.Collection
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.Entry
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.Location
import com.saxonthune.ranktheplanet.domain.ReviewDraft
import com.saxonthune.ranktheplanet.domain.ReviewInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.saxonthune.ranktheplanet.data.op.nowIso

internal class SqlCollectionRepository(
    private val database: AppDatabase,
    private val opLogWriter: OpLogWriter,
    private val locationRepo: SqlLocationRepository,
) : CollectionRepository {

    override fun observeAll(): Flow<List<Collection>> =
        database.collectionQueries.observeAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observe(id: CollectionId): Flow<Collection?> =
        database.collectionQueries.observeById(id.value)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toDomain() }

    override suspend fun create(
        name: String,
        description: String?,
        appearance: Appearance,
        powerRanking: Boolean,
    ): Result<Collection> = withContext(Dispatchers.Default) {
        runCatching {
            database.transactionWithResult {
                val id = generateUuid()
                val now = nowIso()
                database.collectionQueries.insert(
                    id = id,
                    name = name,
                    description = description,
                    appearance_color = appearance.color,
                    appearance_pin_style = appearance.pinStyle,
                    template_version = 0L,
                    is_visible = 1L,
                    power_ranking = if (powerRanking) 1L else 0L,
                    created = now,
                    last_modified = now,
                )
                opLogWriter.append(Op.CollectionCreated(
                    collectionId = id,
                    name = name,
                    description = description,
                    appearanceColor = appearance.color,
                    appearancePinStyle = appearance.pinStyle,
                    powerRanking = powerRanking,
                ))
                Collection(
                    id = CollectionId(id),
                    name = name,
                    description = description,
                    appearance = appearance,
                    templateVersion = 0,
                    isVisible = true,
                    powerRanking = powerRanking,
                    created = now,
                    lastModified = now,
                )
            }
        }
    }

    override suspend fun editMetadata(
        id: CollectionId,
        name: String,
        description: String?,
        appearance: Appearance,
        powerRanking: Boolean,
    ): Result<Collection> = withContext(Dispatchers.Default) {
        runCatching {
            database.transactionWithResult {
                val current = database.collectionQueries.observeById(id.value)
                    .executeAsOneOrNull()
                    ?: throw IllegalArgumentException("Collection not found: ${id.value}")
                val now = nowIso()
                database.collectionQueries.updateMetadata(
                    name = name,
                    description = description,
                    appearance_color = appearance.color,
                    appearance_pin_style = appearance.pinStyle,
                    power_ranking = if (powerRanking) 1L else 0L,
                    last_modified = now,
                    id = id.value,
                )
                opLogWriter.append(Op.CollectionMetadataEdited(
                    collectionId = id.value,
                    name = name,
                    description = description,
                    appearanceColor = appearance.color,
                    appearancePinStyle = appearance.pinStyle,
                    powerRanking = powerRanking,
                ))
                Collection(
                    id = id,
                    name = name,
                    description = description,
                    appearance = appearance,
                    templateVersion = current.template_version.toInt(),
                    isVisible = current.is_visible == 1L,
                    powerRanking = powerRanking,
                    created = current.created,
                    lastModified = now,
                )
            }
        }
    }

    override suspend fun addEntry(
        collectionId: CollectionId,
        location: Location,
        review: ReviewDraft,
    ): Result<Entry> = withContext(Dispatchers.Default) {
        runCatching {
            database.transactionWithResult {
                val collection = database.collectionQueries.observeById(collectionId.value)
                    .executeAsOneOrNull()
                    ?: throw IllegalArgumentException("Collection not found: ${collectionId.value}")

                if (review.data.isNotEmpty() && review.templateVersion != collection.template_version.toInt()) {
                    throw IllegalStateException(
                        "Template version mismatch: caller=${review.templateVersion} collection=${collection.template_version}"
                    )
                }

                val finalLocation = locationRepo.upsertInternal(location)

                val existingEntryId = database.entryQueries
                    .selectByCollectionAndLocation(collectionId.value, finalLocation.id.value)
                    .executeAsOneOrNull()
                if (existingEntryId != null) {
                    throw IllegalStateException(
                        "Entry already exists for collection=${collectionId.value} location=${finalLocation.id.value}"
                    )
                }

                val entryId = generateUuid()
                val now = nowIso()
                val dataJson = if (review.data.isEmpty()) null else encodeReviewData(review.data)
                val templateVersion = if (review.data.isEmpty()) null else review.templateVersion.toLong()

                database.entryQueries.insert(
                    id = entryId,
                    collection_id = collectionId.value,
                    location_id = finalLocation.id.value,
                    data_ = dataJson,
                    recorded_template_version = templateVersion,
                    created = now,
                    last_modified = now,
                )
                database.collectionQueries.bumpLastModified(now, collectionId.value)
                opLogWriter.append(Op.EntryAdded(
                    entryId = entryId,
                    collectionId = collectionId.value,
                    locationId = finalLocation.id.value,
                    data = dataJson,
                    templateVersion = templateVersion?.toInt(),
                ))

                Entry(
                    id = EntryId(entryId),
                    collectionId = collectionId,
                    location = finalLocation,
                    review = if (review.data.isEmpty()) null else ReviewInstance(
                        data = review.data,
                        recordedTemplateVersion = review.templateVersion,
                        created = now,
                        lastModified = now,
                    ),
                    added = now,
                )
            }
        }
    }

    override suspend fun removeEntry(entryId: EntryId): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            database.transaction {
                val collectionId = database.entryQueries.observeById(entryId.value) { _, collection_id, _, _, _, _, _, _, _, _, _, _, _, _, _ ->
                    collection_id
                }.executeAsOneOrNull()
                    ?: throw IllegalArgumentException("Entry not found: ${entryId.value}")
                val now = nowIso()
                database.entryQueries.deleteById(entryId.value)
                database.collectionQueries.bumpLastModified(now, collectionId)
                opLogWriter.append(Op.EntryRemoved(entryId = entryId.value, collectionId = collectionId))
            }
        }
    }
}

private fun com.saxonthune.ranktheplanet.db.Collection.toDomain() = Collection(
    id = CollectionId(id),
    name = name,
    description = description,
    appearance = Appearance(color = appearance_color, pinStyle = appearance_pin_style),
    templateVersion = template_version.toInt(),
    isVisible = is_visible == 1L,
    powerRanking = power_ranking == 1L,
    created = created,
    lastModified = last_modified,
)
