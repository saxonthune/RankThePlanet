package com.saxonthune.ranktheplanet.data.sql

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.op.Op
import com.saxonthune.ranktheplanet.data.op.OpLogWriter
import com.saxonthune.ranktheplanet.db.AppDatabase
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.Entry
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.ReviewInstance
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.saxonthune.ranktheplanet.data.op.nowIso

internal class SqlEntryRepository(
    private val database: AppDatabase,
    private val opLogWriter: OpLogWriter,
) : EntryRepository {

    override fun observeAll(): Flow<List<Entry>> =
        database.entryQueries.observeAll(::entryMapper)
            .asFlow()
            .mapToList(Dispatchers.Default)

    override fun observeByCollection(collectionId: CollectionId): Flow<List<Entry>> =
        database.entryQueries.observeByCollection(collectionId.value, ::entryMapper)
            .asFlow()
            .mapToList(Dispatchers.Default)

    override fun observe(entryId: EntryId): Flow<Entry?> =
        database.entryQueries.observeById(entryId.value, ::entryMapper)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)

    override suspend fun editReview(
        entryId: EntryId,
        data: Map<String, String>,
    ): Result<Entry> = withContext(Dispatchers.Default) {
        runCatching {
            database.transactionWithResult {
                val entryRow = database.entryQueries.observeById(entryId.value, ::entryMapper)
                    .executeAsOneOrNull()
                    ?: throw IllegalArgumentException("Entry not found: ${entryId.value}")

                val collection = database.collectionQueries.observeById(entryRow.collectionId.value)
                    .executeAsOneOrNull()
                    ?: throw IllegalStateException("Collection not found: ${entryRow.collectionId.value}")

                val currentVersion = collection.template_version.toInt()
                val currentFieldNames = database.templateFieldQueries
                    .selectByCollectionAndVersion(entryRow.collectionId.value, collection.template_version)
                    .executeAsList()
                    .map { it.name }
                    .toSet()
                val reconciled = data.filterKeys { it in currentFieldNames }

                val now = nowIso()
                val dataJson = encodeReviewData(reconciled)
                database.entryQueries.updateReviewData(
                    data_ = dataJson,
                    recorded_template_version = currentVersion.toLong(),
                    last_modified = now,
                    id = entryId.value,
                )
                database.collectionQueries.bumpLastModified(now, entryRow.collectionId.value)
                opLogWriter.append(Op.ReviewEdited(
                    entryId = entryId.value,
                    data = dataJson,
                    templateVersion = currentVersion,
                ))

                val existingReview = entryRow.review
                val updatedReview = ReviewInstance(
                    data = reconciled.toImmutableMap(),
                    recordedTemplateVersion = currentVersion,
                    created = existingReview?.created ?: now,
                    lastModified = now,
                )
                entryRow.copy(review = updatedReview)
            }
        }
    }
}
