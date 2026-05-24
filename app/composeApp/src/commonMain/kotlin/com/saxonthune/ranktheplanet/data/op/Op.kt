package com.saxonthune.ranktheplanet.data.op

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal val opJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

internal sealed interface Op {
    val kind: String

    @Serializable
    data class CollectionCreated(
        val collectionId: String,
        val name: String,
        val description: String?,
        val appearanceColor: String,
        val appearancePinStyle: String,
    ) : Op { override val kind = "collection.created" }

    @Serializable
    data class EntryAdded(
        val entryId: String,
        val collectionId: String,
        val locationId: String,
        val data: String?,
        val templateVersion: Int?,
    ) : Op { override val kind = "entry.added" }

    @Serializable
    data class EntryRemoved(
        val entryId: String,
        val collectionId: String,
    ) : Op { override val kind = "entry.removed" }

    @Serializable
    data class ReviewEdited(
        val entryId: String,
        val data: String,
        val templateVersion: Int,
    ) : Op { override val kind = "entry.review.edited" }

    @Serializable
    data class TemplateDefined(
        val collectionId: String,
        val version: Int,
        val fieldsJson: String,
    ) : Op { override val kind = "template.defined" }

    @Serializable
    data class TemplateEdited(
        val collectionId: String,
        val newVersion: Int,
        val fieldsJson: String,
    ) : Op { override val kind = "template.edited" }

    @Serializable
    data class LocationUpserted(
        val locationId: String,
        val sourceType: String,
        val sourceId: String,
        val lat: Double,
        val lng: Double,
        val displayName: String,
    ) : Op { override val kind = "location.upserted" }

    @Serializable
    data class LocationMerged(
        val keepId: String,
        val dropId: String,
    ) : Op { override val kind = "location.merged" }
}

internal fun Op.toPayloadJson(): String = when (this) {
    is Op.CollectionCreated -> opJson.encodeToString(this)
    is Op.EntryAdded -> opJson.encodeToString(this)
    is Op.EntryRemoved -> opJson.encodeToString(this)
    is Op.ReviewEdited -> opJson.encodeToString(this)
    is Op.TemplateDefined -> opJson.encodeToString(this)
    is Op.TemplateEdited -> opJson.encodeToString(this)
    is Op.LocationUpserted -> opJson.encodeToString(this)
    is Op.LocationMerged -> opJson.encodeToString(this)
}
