package com.saxonthune.ranktheplanet.domain.io

import com.saxonthune.ranktheplanet.domain.Appearance
import com.saxonthune.ranktheplanet.domain.AppearancePalette
import com.saxonthune.ranktheplanet.domain.Collection
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.Entry
import com.saxonthune.ranktheplanet.domain.FieldType
import com.saxonthune.ranktheplanet.domain.Location
import com.saxonthune.ranktheplanet.domain.LocationId
import com.saxonthune.ranktheplanet.domain.ReviewInstance
import com.saxonthune.ranktheplanet.domain.ReviewTemplate
import com.saxonthune.ranktheplanet.domain.SourceType
import com.saxonthune.ranktheplanet.domain.TemplateField
import com.saxonthune.ranktheplanet.domain.TemplateFieldConfig
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

object GeoJsonCodec {

    private val bundleVersion = 1

    fun encode(
        collection: Collection,
        template: ReviewTemplate?,
        entries: List<Entry>,
    ): String {
        val root = buildJsonObject {
            put("type", "FeatureCollection")
            putJsonObject("rtp") {
                put("bundleVersion", bundleVersion)
                putJsonObject("collection") {
                    put("name", collection.name)
                    put("description", collection.description ?: "")
                    putJsonObject("appearance") {
                        put("color", collection.appearance.color)
                        put("pinStyle", collection.appearance.pinStyle)
                    }
                    put("powerRanking", collection.powerRanking)
                    put("created", collection.created)
                    put("lastModified", collection.lastModified)
                }
                if (template != null) {
                    put("template", encodeTemplate(template))
                } else {
                    put("template", JsonNull)
                }
            }
            put("features", JsonArray(entries.map { entry -> encodeFeature(entry) }))
        }
        return Json.encodeToString(JsonObject.serializer(), root)
    }

    private fun encodeTemplate(template: ReviewTemplate): JsonObject = buildJsonObject {
        put("version", template.version)
        put("summaryField", template.summaryField ?: "")
        putJsonArray("fields") {
            for (field in template.fields) {
                add(encodeField(field))
            }
        }
    }

    private fun encodeField(field: TemplateField): JsonObject = buildJsonObject {
        put("name", field.name)
        put("label", field.label)
        put("type", field.type.name)
        put("ordinal", field.ordinal)
        put("config", encodeConfig(field.config))
    }

    private fun encodeConfig(config: TemplateFieldConfig?): JsonObject = buildJsonObject {
        when (config) {
            is TemplateFieldConfig.Score -> {
                put("kind", "Score")
                put("min", config.min)
                put("max", config.max)
                put("step", config.step)
                put("render", config.render)
            }
            is TemplateFieldConfig.TextField -> put("kind", "Text")
            is TemplateFieldConfig.Enum -> {
                put("kind", "Enum")
                putJsonArray("options") { config.options.forEach { add(JsonPrimitive(it)) } }
            }
            is TemplateFieldConfig.BooleanField -> put("kind", "Boolean")
            is TemplateFieldConfig.Date -> put("kind", "Date")
            null -> put("kind", JsonNull)
        }
    }

    private fun encodeFeature(entry: Entry): JsonObject = buildJsonObject {
        put("type", "Feature")
        putJsonObject("geometry") {
            put("type", "Point")
            putJsonArray("coordinates") {
                add(JsonPrimitive(entry.location.coordinates.lng))
                add(JsonPrimitive(entry.location.coordinates.lat))
            }
        }
        putJsonObject("properties") {
            put("rtp:displayName", entry.location.displayName)
            put("rtp:sourceType", entry.location.sourceType.name)
            put("rtp:sourceId", entry.location.sourceId)
            put("rtp:address", entry.location.address ?: "")
            put("rtp:cachedMetadata", entry.location.cachedMetadata ?: "")
            put("rtp:refreshable", entry.location.refreshable)
            put("rtp:added", entry.added)
            if (entry.review != null) {
                putJsonObject("rtp:review") {
                    put("recordedTemplateVersion", entry.review.recordedTemplateVersion)
                    put("created", entry.review.created)
                    put("lastModified", entry.review.lastModified)
                    putJsonObject("data") {
                        entry.review.data.forEach { (k, v) -> put(k, v) }
                    }
                }
            }
        }
    }

    fun decode(json: String): Result<PortableCollection> = runCatching {
        val root = Json.parseToJsonElement(json).jsonObject
        val type = root["type"]?.jsonPrimitive?.content
        if (type != "FeatureCollection") error("Expected FeatureCollection, got $type")

        val rtpBlock = root["rtp"]?.jsonObject
        val collectionMeta = rtpBlock?.get("collection")?.jsonObject
        val templateMeta = rtpBlock?.get("template")?.takeIf { it !is JsonNull }?.jsonObject

        val collection = decodeCollection(collectionMeta)
        val template = templateMeta?.let { decodeTemplate(it) }

        val features = root["features"]?.jsonArray ?: JsonArray(emptyList())
        val entries = features.map { feat ->
            val featureObj = feat.jsonObject
            val geom = featureObj["geometry"]?.jsonObject
                ?: error("Feature missing geometry")
            if (geom["type"]?.jsonPrimitive?.content != "Point") {
                error("Non-Point geometry is not supported")
            }
            val coords = geom["coordinates"]?.jsonArray ?: error("Missing coordinates")
            val lng = coords[0].jsonPrimitive.double
            val lat = coords[1].jsonPrimitive.double

            val props = featureObj["properties"]?.jsonObject ?: JsonObject(emptyMap())
            val sourceTypeStr = props["rtp:sourceType"]?.jsonPrimitive?.content ?: "Manual"
            val sourceType = try {
                SourceType.valueOf(sourceTypeStr)
            } catch (e: IllegalArgumentException) {
                error("Unknown SourceType: $sourceTypeStr")
            }

            val location = Location(
                id = LocationId(""),
                coordinates = Coordinates(lat = lat, lng = lng),
                displayName = props["rtp:displayName"]?.jsonPrimitive?.content ?: "",
                sourceType = sourceType,
                sourceId = props["rtp:sourceId"]?.jsonPrimitive?.content ?: "",
                address = props["rtp:address"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
                cachedMetadata = props["rtp:cachedMetadata"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
                refreshable = props["rtp:refreshable"]?.jsonPrimitive?.boolean ?: false,
            )

            val review = props["rtp:review"]?.takeIf { it !is JsonNull }?.jsonObject?.let { r ->
                val data = r["data"]?.jsonObject?.entries
                    ?.associate { (k, v) -> k to v.jsonPrimitive.content }
                    ?.toImmutableMap()
                    ?: emptyMap<String, String>().toImmutableMap()
                ReviewInstance(
                    data = data,
                    recordedTemplateVersion = r["recordedTemplateVersion"]!!.jsonPrimitive.int,
                    created = r["created"]!!.jsonPrimitive.content,
                    lastModified = r["lastModified"]!!.jsonPrimitive.content,
                )
            }

            val added = props["rtp:added"]?.jsonPrimitive?.content ?: ""
            PortableEntry(location = location, review = review)
        }

        PortableCollection(collection = collection, template = template, entries = entries)
    }

    private fun decodeCollection(meta: JsonObject?): Collection {
        val appearance = meta?.get("appearance")?.jsonObject?.let {
            Appearance(
                color = it["color"]?.jsonPrimitive?.content ?: AppearancePalette.default.color,
                pinStyle = it["pinStyle"]?.jsonPrimitive?.content ?: AppearancePalette.default.pinStyle,
            )
        } ?: AppearancePalette.default

        return Collection(
            id = CollectionId(""),
            name = meta?.get("name")?.jsonPrimitive?.content ?: "",
            description = meta?.get("description")?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
            appearance = appearance,
            templateVersion = 0,
            isVisible = true,
            powerRanking = meta?.get("powerRanking")?.jsonPrimitive?.boolean ?: false,
            created = meta?.get("created")?.jsonPrimitive?.content ?: "",
            lastModified = meta?.get("lastModified")?.jsonPrimitive?.content ?: "",
        )
    }

    private fun decodeTemplate(meta: JsonObject): ReviewTemplate {
        val version = meta["version"]?.jsonPrimitive?.int ?: 0
        val summaryField = meta["summaryField"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() }
        val fields = meta["fields"]?.jsonArray?.map { decodeField(it.jsonObject) }
            ?.toImmutableList()
            ?: persistentListOf()
        return ReviewTemplate(
            collectionId = CollectionId(""),
            version = version,
            fields = fields,
            summaryField = summaryField,
        )
    }

    private fun decodeField(obj: JsonObject): TemplateField {
        val name = obj["name"]!!.jsonPrimitive.content
        val label = obj["label"]?.jsonPrimitive?.content ?: name
        val type = FieldType.valueOf(obj["type"]!!.jsonPrimitive.content)
        val ordinal = obj["ordinal"]?.jsonPrimitive?.int ?: 0
        val config = obj["config"]?.takeIf { it !is JsonNull }?.jsonObject?.let { decodeConfig(it) }
        return TemplateField(name = name, label = label, type = type, config = config, ordinal = ordinal)
    }

    private fun decodeConfig(obj: JsonObject): TemplateFieldConfig? {
        return when (val kind = obj["kind"]?.jsonPrimitive?.content) {
            "Score" -> TemplateFieldConfig.Score(
                min = obj["min"]?.jsonPrimitive?.double ?: 0.0,
                max = obj["max"]?.jsonPrimitive?.double ?: 5.0,
                step = obj["step"]?.jsonPrimitive?.double ?: 0.5,
                render = obj["render"]?.jsonPrimitive?.content ?: "stars",
            )
            "Text" -> TemplateFieldConfig.TextField
            "Enum" -> TemplateFieldConfig.Enum(
                options = obj["options"]!!.jsonArray.map { it.jsonPrimitive.content }.toImmutableList()
            )
            "Boolean" -> TemplateFieldConfig.BooleanField
            "Date" -> TemplateFieldConfig.Date
            null -> null
            else -> error("Unknown config kind: $kind")
        }
    }
}
