package com.saxonthune.ranktheplanet.data

import com.saxonthune.ranktheplanet.domain.*
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    fun observeAll(): Flow<List<Collection>>
    fun observe(id: CollectionId): Flow<Collection?>
    suspend fun create(name: String, appearance: Appearance): Result<Collection>
    suspend fun addEntry(collectionId: CollectionId, location: Location, review: ReviewDraft): Result<Entry>
    suspend fun removeEntry(entryId: EntryId): Result<Unit>
}

interface EntryRepository {
    fun observeByCollection(collectionId: CollectionId): Flow<List<Entry>>
    fun observe(entryId: EntryId): Flow<Entry?>
    suspend fun editReview(entryId: EntryId, data: Map<String, String>): Result<Entry>
}

interface LocationRepository {
    suspend fun findByIdentity(sourceType: SourceType, sourceId: String): Location?
    suspend fun upsert(location: Location): Result<Location>
    suspend fun merge(keep: LocationId, drop: LocationId): Result<Location>
}

interface TemplateRepository {
    fun observe(collectionId: CollectionId): Flow<ReviewTemplate?>
    suspend fun define(collectionId: CollectionId, fields: List<TemplateField>): Result<ReviewTemplate>
    suspend fun edit(collectionId: CollectionId, fields: List<TemplateField>): Result<ReviewTemplate>
}
