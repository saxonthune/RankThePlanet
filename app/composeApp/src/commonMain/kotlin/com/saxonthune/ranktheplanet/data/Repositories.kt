package com.saxonthune.ranktheplanet.data

import com.saxonthune.ranktheplanet.domain.Appearance
import com.saxonthune.ranktheplanet.domain.Collection
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.Entry
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.Location
import com.saxonthune.ranktheplanet.domain.LocationId
import com.saxonthune.ranktheplanet.domain.ReviewDraft
import com.saxonthune.ranktheplanet.domain.ReviewTemplate
import com.saxonthune.ranktheplanet.domain.SourceType
import com.saxonthune.ranktheplanet.domain.TemplateField
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    fun observeAll(): Flow<List<Collection>>
    fun observe(id: CollectionId): Flow<Collection?>
    suspend fun create(name: String, description: String? = null, appearance: Appearance): Result<Collection>
    suspend fun addEntry(collectionId: CollectionId, location: Location, review: ReviewDraft): Result<Entry>
    suspend fun removeEntry(entryId: EntryId): Result<Unit>
}

interface EntryRepository {
    fun observeAll(): Flow<List<Entry>>
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
