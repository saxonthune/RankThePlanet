package com.saxonthune.ranktheplanet.data

import com.saxonthune.ranktheplanet.domain.AppearancePalette
import com.saxonthune.ranktheplanet.domain.BuiltInTemplates
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.ReviewDraft
import com.saxonthune.ranktheplanet.domain.io.GeoJsonCodec
import com.saxonthune.ranktheplanet.domain.io.KmlCodec
import com.saxonthune.ranktheplanet.domain.io.PortFormat
import com.saxonthune.ranktheplanet.data.op.generateUuid
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException

interface CollectionPortIoService {
    suspend fun export(collectionId: CollectionId, format: PortFormat): Result<ExportedBundle>
    suspend fun import(
        text: String,
        format: PortFormat,
        onProgress: (ImportProgress) -> Unit = {},
    ): Result<CollectionId>
}

data class ImportProgress(val completed: Int, val total: Int)

data class ExportedBundle(
    val text: String,
    val suggestedFilename: String,
    val format: PortFormat,
)

class DefaultCollectionPortIoService(
    private val collections: CollectionRepository,
    private val entries: EntryRepository,
    private val templates: TemplateRepository,
    private val locations: LocationRepository,
) : CollectionPortIoService {

    override suspend fun export(collectionId: CollectionId, format: PortFormat): Result<ExportedBundle> =
        runCatching {
            val collection = collections.observe(collectionId).first()
                ?: throw NoSuchElementException("Collection not found: ${collectionId.value}")
            val template = templates.observe(collectionId).first()
            val entryList = entries.observeByCollection(collectionId).first()

            val text = when (format) {
                PortFormat.Kml -> KmlCodec.encode(collection, template, entryList)
                PortFormat.GeoJson -> GeoJsonCodec.encode(collection, template, entryList)
            }

            val baseName = collection.name
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trimEnd('-')
                .ifEmpty { "collection-${collectionId.value}" }

            ExportedBundle(
                text = text,
                suggestedFilename = "$baseName.${format.extension}",
                format = format,
            )
        }

    override suspend fun import(
        text: String,
        format: PortFormat,
        onProgress: (ImportProgress) -> Unit,
    ): Result<CollectionId> {
        var newId: CollectionId? = null
        val result = runCatching {
            require(text.length <= MAX_IMPORT_CHARS) { "Import is larger than the 25 MB safety limit" }
            val portable = when (format) {
                PortFormat.Kml -> KmlCodec.decode(text)
                PortFormat.GeoJson -> GeoJsonCodec.decode(text)
            }.getOrElse { throw it }

            val appearance = nextAvailableAppearance()
            val newCollection = collections.create(
                name = portable.collection.name,
                description = portable.collection.description,
                appearance = appearance,
            ).getOrElse { throw it }
            newId = newCollection.id
            val importedId = newCollection.id

            if (portable.template != null) {
                templates.define(importedId, portable.template.fields).getOrElse { throw it }
            } else {
                templates.define(importedId, BuiltInTemplates.wishlist.fields).getOrElse { throw it }
            }

            onProgress(ImportProgress(completed = 0, total = portable.entries.size))
            portable.entries.forEachIndexed { index, portableEntry ->
                currentCoroutineContext().ensureActive()
                val rawLoc = portableEntry.location
                // Locations with no stable sourceId must each get a unique identity to avoid accidental reuse.
                val loc = if (rawLoc.sourceId.isEmpty()) rawLoc.copy(sourceId = generateUuid()) else rawLoc

                val existing = locations.findByIdentity(loc.sourceType, loc.sourceId)
                val finalLoc = existing
                    ?: locations.upsert(loc).getOrElse { throw it }

                val review = if (portableEntry.review != null) {
                    ReviewDraft(
                        data = portableEntry.review.data,
                        templateVersion = portableEntry.review.recordedTemplateVersion,
                    )
                } else {
                    ReviewDraft(data = persistentMapOf())
                }

                collections.addEntry(importedId, finalLoc, review).getOrElse { throw it }
                onProgress(ImportProgress(completed = index + 1, total = portable.entries.size))
            }

            importedId
        }
        if (result.isFailure && newId != null) {
            collections.discardImport(newId!!)
        }
        val failure = result.exceptionOrNull()
        if (failure is CancellationException) throw failure
        return result
    }

    private suspend fun nextAvailableAppearance() =
        collections.observeAll().first().map { it.appearance.color }.toSet().let { usedColors ->
            AppearancePalette.swatches.firstOrNull { it.color !in usedColors }
                ?: AppearancePalette.default
        }
}

private const val MAX_IMPORT_CHARS = 25 * 1024 * 1024
