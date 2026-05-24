package com.saxonthune.ranktheplanet.domain

import kotlin.jvm.JvmInline
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.serialization.Serializable

sealed interface TemplateFieldConfig {
    data class Score(
        val min: Double = 0.0,
        val max: Double = 5.0,
        val step: Double = 0.5,
        val render: String = "stars",
    ) : TemplateFieldConfig
    data class Text(val multiline: Boolean = false) : TemplateFieldConfig
    data class Enum(val options: ImmutableList<String>) : TemplateFieldConfig
    data object BooleanField : TemplateFieldConfig
    data object Date : TemplateFieldConfig
    data object PowerRanking : TemplateFieldConfig
}

@JvmInline value class CollectionId(val value: String)
@JvmInline value class LocationId(val value: String)
@JvmInline value class EntryId(val value: String)

data class Coordinates(val lat: Double, val lng: Double)

enum class SourceType { Google, Osm, Apple, Manual, Fake }

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
    val label: String = name,
    val type: FieldType,
    val config: TemplateFieldConfig? = null,
    val required: Boolean = false,
    val ordinal: Int = 0,
)

data class ReviewTemplate(
    val collectionId: CollectionId,
    val version: Int,
    val fields: ImmutableList<TemplateField>,
    val summaryField: String? = null,
)

data class ReviewInstance(
    val data: ImmutableMap<String, String>,
    val recordedTemplateVersion: Int,
    val created: String,
    val lastModified: String,
)

data class ReviewDraft(
    val data: ImmutableMap<String, String>,
    val templateVersion: Int = 0,
)

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

@Serializable
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
