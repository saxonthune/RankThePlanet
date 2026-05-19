package com.saxonthune.ranktheplanet.domain

import kotlin.jvm.JvmInline
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet

@JvmInline value class CollectionId(val value: String)
@JvmInline value class LocationId(val value: String)
@JvmInline value class EntryId(val value: String)

data class Coordinates(val lat: Double, val lng: Double)

enum class SourceType { Google, Osm, Apple, Manual }

data class Location(
    val id: LocationId,
    val coordinates: Coordinates,
    val displayName: String,
    val sourceType: SourceType,
    val sourceId: String,
    val cachedMetadata: String?,
    val refreshable: Boolean
)

data class Appearance(val color: String, val pinStyle: String)

enum class FieldType { Score, Text, Enum, Boolean, Date, PowerRanking }

data class TemplateField(
    val name: String,
    val type: FieldType,
    val config: String? = null,
    val required: Boolean = false
)

data class ReviewTemplate(
    val collectionId: CollectionId,
    val version: Int,
    val fields: ImmutableList<TemplateField>
)

data class ReviewInstance(
    val data: ImmutableMap<String, String>,
    val recordedTemplateVersion: Int,
    val created: String,
    val lastModified: String,
)

data class ReviewDraft(val data: ImmutableMap<String, String>)

data class Entry(
    val id: EntryId,
    val collectionId: CollectionId,
    val location: Location,
    val review: ReviewInstance?,
    val added: String
)

data class Collection(
    val id: CollectionId,
    val name: String,
    val description: String? = null,
    val appearance: Appearance,
    val templateVersion: Int,
    val isVisible: Boolean,
    val created: String,
    val lastModified: String
)

data class Viewport(
    val centerLat: Double,
    val centerLng: Double,
    val zoom: Double,
    val bearing: Double
)

data class VisiblePin(
    val entryId: EntryId,
    val coordinates: Coordinates,
    val collectionColor: String,
    val visited: Boolean
)

data class MapOverviewState(
    val viewport: Viewport,
    val visiblePins: ImmutableList<VisiblePin>,
    val collectionFilter: ImmutableSet<CollectionId>
)
