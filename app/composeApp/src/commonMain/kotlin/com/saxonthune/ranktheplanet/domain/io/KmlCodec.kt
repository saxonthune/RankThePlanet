package com.saxonthune.ranktheplanet.domain.io

// Minimal hand-rolled KML parser. Pathological XML — CDATA in unusual places, namespace prefixes
// other than the default + rtp:, or comments containing </Placemark> — is out of scope for v1.

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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

object KmlCodec {

    fun encode(
        collection: Collection,
        template: ReviewTemplate?,
        entries: List<Entry>,
    ): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<kml xmlns="http://www.opengis.net/kml/2.2">""")
        appendLine("  <Document>")
        appendLine("    <name>${escape(collection.name)}</name>")
        appendLine("    <description>${escape(collection.description ?: "")}</description>")
        appendLine("""    <ExtendedData xmlns:rtp="https://ranktheplanet.app/ns">""")
        appendLine("""      <rtp:appearance color="${escape(collection.appearance.color)}" pinStyle="${escape(collection.appearance.pinStyle)}"/>""")
        appendLine("      <rtp:powerRanking>${collection.powerRanking}</rtp:powerRanking>")
        appendLine("      <rtp:created>${escape(collection.created)}</rtp:created>")
        appendLine("      <rtp:lastModified>${escape(collection.lastModified)}</rtp:lastModified>")
        if (template != null) {
            val summaryAttr = if (template.summaryField != null) """ summaryField="${escape(template.summaryField)}"""" else ""
            appendLine("""      <rtp:template version="${template.version}"$summaryAttr>""")
            for (field in template.fields) {
                appendLine("""        <rtp:field name="${escape(field.name)}" label="${escape(field.label)}" type="${field.type.name}" ordinal="${field.ordinal}">""")
                appendLine(encodeConfig(field.config))
                appendLine("        </rtp:field>")
            }
            appendLine("      </rtp:template>")
        }
        appendLine("    </ExtendedData>")
        for (entry in entries) {
            appendLine("    <Placemark>")
            appendLine("      <name>${escape(entry.location.displayName)}</name>")
            appendLine("      <description>${escape(entry.location.address ?: "")}</description>")
            val lng = entry.location.coordinates.lng
            val lat = entry.location.coordinates.lat
            appendLine("      <Point><coordinates>$lng,$lat</coordinates></Point>")
            appendLine("""      <ExtendedData xmlns:rtp="https://ranktheplanet.app/ns">""")
            appendLine("        <rtp:sourceType>${entry.location.sourceType.name}</rtp:sourceType>")
            appendLine("        <rtp:sourceId>${escape(entry.location.sourceId)}</rtp:sourceId>")
            appendLine("        <rtp:refreshable>${entry.location.refreshable}</rtp:refreshable>")
            appendLine("        <rtp:cachedMetadata>${escape(entry.location.cachedMetadata ?: "")}</rtp:cachedMetadata>")
            appendLine("        <rtp:added>${escape(entry.added)}</rtp:added>")
            if (entry.review != null) {
                appendLine("""        <rtp:review recordedTemplateVersion="${entry.review.recordedTemplateVersion}" created="${escape(entry.review.created)}" lastModified="${escape(entry.review.lastModified)}">""")
                for ((name, value) in entry.review.data) {
                    appendLine("""          <rtp:value name="${escape(name)}">${escape(value)}</rtp:value>""")
                }
                appendLine("        </rtp:review>")
            }
            appendLine("      </ExtendedData>")
            appendLine("    </Placemark>")
        }
        appendLine("  </Document>")
        append("</kml>")
    }

    private fun encodeConfig(config: TemplateFieldConfig?): String = when (config) {
        is TemplateFieldConfig.Score ->
            """          <rtp:config kind="Score" min="${config.min}" max="${config.max}" step="${config.step}" render="${escape(config.render)}"/>"""
        is TemplateFieldConfig.TextField ->
            """          <rtp:config kind="Text"/>"""
        is TemplateFieldConfig.Enum -> buildString {
            appendLine("""          <rtp:config kind="Enum">""")
            for (opt in config.options) {
                appendLine("            <rtp:option>${escape(opt)}</rtp:option>")
            }
            append("          </rtp:config>")
        }
        is TemplateFieldConfig.BooleanField ->
            """          <rtp:config kind="Boolean"/>"""
        is TemplateFieldConfig.Date ->
            """          <rtp:config kind="Date"/>"""
        null -> ""
    }

    fun decode(xml: String): Result<PortableCollection> = runCatching {
        val docBlock = blockContent(xml, "Document") ?: error("Missing <Document>")

        val collectionName = simpleText(docBlock, "name") ?: ""
        val collectionDesc = simpleText(docBlock, "description")?.takeIf { it.isNotEmpty() }

        val docExtData = rtpExtDataBlock(docBlock)
        val appearance: Appearance
        val powerRanking: Boolean
        val created: String
        val lastModified: String
        val template: ReviewTemplate?

        if (docExtData != null) {
            val appearanceMatch = Regex("""<rtp:appearance\s[^/]*color="([^"]*)"[^/]*pinStyle="([^"]*)"""").find(docExtData)
            val pinStyleFirst = Regex("""<rtp:appearance\s[^/]*pinStyle="([^"]*)"[^/]*color="([^"]*)"""").find(docExtData)
            val (color, pinStyle) = if (appearanceMatch != null) {
                appearanceMatch.groupValues[1] to appearanceMatch.groupValues[2]
            } else if (pinStyleFirst != null) {
                pinStyleFirst.groupValues[2] to pinStyleFirst.groupValues[1]
            } else {
                AppearancePalette.default.color to AppearancePalette.default.pinStyle
            }
            appearance = Appearance(color = unescape(color), pinStyle = unescape(pinStyle))
            powerRanking = rtpSimpleText(docExtData, "powerRanking")?.toBooleanStrictOrNull() ?: false
            created = rtpSimpleText(docExtData, "created") ?: ""
            lastModified = rtpSimpleText(docExtData, "lastModified") ?: ""
            template = decodeTemplate(docExtData)
        } else {
            appearance = AppearancePalette.default
            powerRanking = false
            created = ""
            lastModified = ""
            template = null
        }

        val collection = Collection(
            id = CollectionId(""),
            name = unescape(collectionName),
            description = collectionDesc?.let { unescape(it) },
            appearance = appearance,
            templateVersion = template?.version ?: 0,
            isVisible = true,
            powerRanking = powerRanking,
            created = unescape(created),
            lastModified = unescape(lastModified),
        )

        val placemarkBlocks = allBlocks(docBlock, "Placemark")
        val entries = placemarkBlocks.map { pm -> decodePlacemark(pm) }

        PortableCollection(collection = collection, template = template, entries = entries)
    }

    private fun decodeTemplate(extData: String): ReviewTemplate? {
        val templateBlock = Regex(
            """<rtp:template\s([^>]*?)>([\s\S]*?)</rtp:template>"""
        ).find(extData) ?: return null

        val attrs = templateBlock.groupValues[1]
        val body = templateBlock.groupValues[2]
        val version = attrValue(attrs, "version")?.toIntOrNull() ?: 0
        val summaryField = attrValue(attrs, "summaryField")?.takeIf { it.isNotEmpty() }

        val fields = Regex(
            """<rtp:field\s([^>]*?)>([\s\S]*?)</rtp:field>"""
        ).findAll(body).map { m ->
            val fa = m.groupValues[1]
            val fb = m.groupValues[2]
            val name = attrValue(fa, "name") ?: error("field missing name")
            val label = attrValue(fa, "label") ?: name
            val typeStr = attrValue(fa, "type") ?: error("field missing type")
            val type = FieldType.valueOf(typeStr)
            val ordinal = attrValue(fa, "ordinal")?.toIntOrNull() ?: 0
            val config = decodeConfig(fb)
            TemplateField(name = unescape(name), label = unescape(label), type = type, config = config, ordinal = ordinal)
        }.toList().toImmutableList()

        return ReviewTemplate(
            collectionId = CollectionId(""),
            version = version,
            fields = fields,
            summaryField = summaryField?.let { unescape(it) },
        )
    }

    private fun decodeConfig(fieldBody: String): TemplateFieldConfig? {
        val configMatch = Regex(
            """<rtp:config\s([^>]*?)(?:/>|>([\s\S]*?)</rtp:config>)"""
        ).find(fieldBody) ?: return null

        val attrs = configMatch.groupValues[1]
        val body = configMatch.groupValues[2]
        return when (val kind = attrValue(attrs, "kind")) {
            "Score" -> TemplateFieldConfig.Score(
                min = attrValue(attrs, "min")?.toDoubleOrNull() ?: 0.0,
                max = attrValue(attrs, "max")?.toDoubleOrNull() ?: 5.0,
                step = attrValue(attrs, "step")?.toDoubleOrNull() ?: 0.5,
                render = attrValue(attrs, "render") ?: "stars",
            )
            "Text" -> TemplateFieldConfig.TextField
            "Enum" -> {
                val options = Regex("""<rtp:option>([\s\S]*?)</rtp:option>""")
                    .findAll(body).map { unescape(it.groupValues[1]) }.toList().toImmutableList()
                TemplateFieldConfig.Enum(options)
            }
            "Boolean" -> TemplateFieldConfig.BooleanField
            "Date" -> TemplateFieldConfig.Date
            null -> null
            else -> error("Unknown config kind: $kind")
        }
    }

    private fun decodePlacemark(pm: String): PortableEntry {
        val displayName = unescape(simpleText(pm, "name") ?: "")
        val address = simpleText(pm, "description")?.let { unescape(it) }?.takeIf { it.isNotEmpty() }

        val pointBlock = blockContent(pm, "Point") ?: error("Non-Point geometry is not supported")
        val coordsRaw = simpleText(pointBlock, "coordinates") ?: error("Missing <coordinates>")
        val parts = coordsRaw.trim().split(",")
        if (parts.size < 2) error("Malformed coordinates: $coordsRaw")
        val lng = parts[0].toDoubleOrNull() ?: error("Malformed coordinates: $coordsRaw")
        val lat = parts[1].toDoubleOrNull() ?: error("Malformed coordinates: $coordsRaw")

        if (blockContent(pm, "LineString") != null || blockContent(pm, "Polygon") != null) {
            error("Non-Point geometry is not supported")
        }

        val extData = rtpExtDataBlock(pm)
        val sourceType: SourceType
        val sourceId: String
        val refreshable: Boolean
        val cachedMetadata: String?
        val added: String
        val review: ReviewInstance?

        if (extData != null) {
            val stStr = rtpSimpleText(extData, "sourceType") ?: "Manual"
            sourceType = try {
                SourceType.valueOf(stStr)
            } catch (e: IllegalArgumentException) {
                error("Unknown SourceType: $stStr")
            }
            sourceId = unescape(rtpSimpleText(extData, "sourceId") ?: "")
            refreshable = rtpSimpleText(extData, "refreshable")?.toBooleanStrictOrNull() ?: false
            cachedMetadata = rtpSimpleText(extData, "cachedMetadata")?.let { unescape(it) }?.takeIf { it.isNotEmpty() }
            added = unescape(rtpSimpleText(extData, "added") ?: "")
            review = decodeReview(extData)
        } else {
            sourceType = SourceType.Manual
            sourceId = ""
            refreshable = false
            cachedMetadata = null
            added = ""
            review = null
        }

        val location = Location(
            id = LocationId(""),
            coordinates = Coordinates(lat = lat, lng = lng),
            displayName = displayName,
            sourceType = sourceType,
            sourceId = sourceId,
            address = address,
            cachedMetadata = cachedMetadata,
            refreshable = refreshable,
        )
        return PortableEntry(location = location, review = review)
    }

    private fun decodeReview(extData: String): ReviewInstance? {
        val reviewMatch = Regex(
            """<rtp:review\s([^>]*?)>([\s\S]*?)</rtp:review>"""
        ).find(extData) ?: return null

        val attrs = reviewMatch.groupValues[1]
        val body = reviewMatch.groupValues[2]
        val version = attrValue(attrs, "recordedTemplateVersion")?.toIntOrNull()
            ?: error("rtp:review missing recordedTemplateVersion")
        val created = unescape(attrValue(attrs, "created") ?: error("rtp:review missing created"))
        val lastModified = unescape(attrValue(attrs, "lastModified") ?: error("rtp:review missing lastModified"))
        val data = Regex("""<rtp:value\s+name="([^"]*)">([\s\S]*?)</rtp:value>""")
            .findAll(body)
            .associate { unescape(it.groupValues[1]) to unescape(it.groupValues[2]) }
            .toImmutableMap()

        return ReviewInstance(data = data, recordedTemplateVersion = version, created = created, lastModified = lastModified)
    }

    // --- XML helpers ---

    private fun escape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun unescape(s: String): String = s
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")

    /** Returns the inner text of `<tag>...</tag>` (first occurrence, not rtp:-prefixed). */
    private fun simpleText(xml: String, tag: String): String? =
        Regex("""<$tag[^>]*>([\s\S]*?)</$tag>""")
            .find(xml)?.groupValues?.get(1)

    /** Returns the inner text of `<rtp:tag>...</rtp:tag>`. */
    private fun rtpSimpleText(xml: String, tag: String): String? =
        Regex("""<rtp:$tag[^>]*>([\s\S]*?)</rtp:$tag>""")
            .find(xml)?.groupValues?.get(1)

    /** Returns the inner content of the first `<tag>...</tag>` block. */
    private fun blockContent(xml: String, tag: String): String? =
        Regex("""<$tag[^>]*>([\s\S]*?)</$tag>""")
            .find(xml)?.groupValues?.get(1)

    /** Returns inner content of all `<tag>...</tag>` blocks. */
    private fun allBlocks(xml: String, tag: String): List<String> =
        Regex("""<$tag[^>]*>([\s\S]*?)</$tag>""")
            .findAll(xml).map { it.groupValues[1] }.toList()

    /** Returns the ExtendedData block containing rtp: elements, or null. */
    private fun rtpExtDataBlock(xml: String): String? =
        Regex("""<ExtendedData[^>]*xmlns:rtp[^>]*>([\s\S]*?)</ExtendedData>""")
            .find(xml)?.groupValues?.get(1)

    /** Extracts attribute value from an attribute string like `name="foo" version="1"`. */
    private fun attrValue(attrs: String, name: String): String? =
        Regex("""$name="([^"]*)"""").find(attrs)?.groupValues?.get(1)
}
