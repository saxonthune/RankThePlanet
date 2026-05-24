package com.saxonthune.ranktheplanet.data.sql

import com.saxonthune.ranktheplanet.data.op.opJson
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.Entry
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.Location
import com.saxonthune.ranktheplanet.domain.LocationId
import com.saxonthune.ranktheplanet.domain.ReviewInstance
import com.saxonthune.ranktheplanet.domain.SourceType
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

internal fun entryMapper(
    entry_id: String,
    collection_id: String,
    @Suppress("UNUSED_PARAMETER") location_id: String,
    data_: String?,
    recorded_template_version: Long?,
    entry_created: String,
    entry_last_modified: String,
    loc_id: String,
    source_type: String,
    source_id: String,
    lat: Double,
    lng: Double,
    display_name: String,
    cached_metadata: String?,
    refreshable: Long,
): Entry = Entry(
    id = EntryId(entry_id),
    collectionId = CollectionId(collection_id),
    location = Location(
        id = LocationId(loc_id),
        coordinates = Coordinates(lat = lat, lng = lng),
        displayName = display_name,
        sourceType = SourceType.valueOf(source_type),
        sourceId = source_id,
        cachedMetadata = cached_metadata,
        refreshable = refreshable == 1L,
    ),
    review = if (data_ != null && recorded_template_version != null) ReviewInstance(
        data = parseReviewData(data_),
        recordedTemplateVersion = recorded_template_version.toInt(),
        created = entry_created,
        lastModified = entry_last_modified,
    ) else null,
    added = entry_created,
)

internal fun parseReviewData(json: String) =
    opJson.decodeFromString(
        MapSerializer(String.serializer(), String.serializer()),
        json,
    ).toImmutableMap()

internal fun encodeReviewData(data: Map<String, String>): String =
    opJson.encodeToString(
        MapSerializer(String.serializer(), String.serializer()),
        data,
    )
