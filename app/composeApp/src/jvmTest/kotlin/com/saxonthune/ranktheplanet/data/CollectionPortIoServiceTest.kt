package com.saxonthune.ranktheplanet.data

import com.saxonthune.ranktheplanet.data.db.createDatabase
import com.saxonthune.ranktheplanet.data.db.createDriver
import com.saxonthune.ranktheplanet.data.sql.SqlRepositories
import com.saxonthune.ranktheplanet.domain.AppearancePalette
import com.saxonthune.ranktheplanet.domain.Appearance
import com.saxonthune.ranktheplanet.domain.BuiltInTemplates
import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.FieldType
import com.saxonthune.ranktheplanet.domain.Location
import com.saxonthune.ranktheplanet.domain.LocationId
import com.saxonthune.ranktheplanet.domain.ReviewDraft
import com.saxonthune.ranktheplanet.domain.SourceType
import com.saxonthune.ranktheplanet.domain.TemplateField
import com.saxonthune.ranktheplanet.domain.TemplateFieldConfig
import com.saxonthune.ranktheplanet.domain.io.PortFormat
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CollectionPortIoServiceTest {

    private fun setup(): Pair<SqlRepositories, DefaultCollectionPortIoService> {
        val db = createDatabase(createDriver("ignored"))
        val repos = SqlRepositories(db, "test-device")
        val service = DefaultCollectionPortIoService(
            repos.collections, repos.entries, repos.templates, repos.locations,
        )
        return repos to service
    }

    @Test
    fun roundTripKml() = runBlocking {
        val (repos, service) = setup()

        val col = repos.collections.create(
            "My Places",
            "A nice collection",
            Appearance("#E2553F", "pin"),
        ).getOrThrow()

        repos.templates.define(col.id, listOf(
            TemplateField("score", "Score", FieldType.Score, TemplateFieldConfig.Score(), 0),
            TemplateField("notes", "Notes", FieldType.Text, TemplateFieldConfig.TextField, 1),
        )).getOrThrow()

        val loc1 = Location(
            id = LocationId(""),
            coordinates = Coordinates(48.8566, 2.3522),
            displayName = "Paris",
            sourceType = SourceType.Manual,
            sourceId = "paris-1",
            address = null,
            cachedMetadata = null,
            refreshable = false,
        )
        repos.collections.addEntry(
            col.id, loc1,
            ReviewDraft(persistentMapOf("score" to "4.5", "notes" to "great"), templateVersion = 1),
        ).getOrThrow()

        val loc2 = Location(
            id = LocationId(""),
            coordinates = Coordinates(51.5074, -0.1278),
            displayName = "London",
            sourceType = SourceType.Manual,
            sourceId = "london-1",
            address = null,
            cachedMetadata = null,
            refreshable = false,
        )
        repos.collections.addEntry(col.id, loc2, ReviewDraft(persistentMapOf())).getOrThrow()

        val bundle = service.export(col.id, PortFormat.Kml).getOrThrow()
        val newId = service.import(bundle.text, PortFormat.Kml).getOrThrow()

        assertTrue(newId != col.id)

        val newColl = repos.collections.observe(newId).first()!!
        assertEquals("My Places", newColl.name)
        assertEquals("A nice collection", newColl.description)

        val newTemplate = repos.templates.observe(newId).first()!!
        assertEquals(2, newTemplate.fields.size)
        assertEquals("score", newTemplate.fields[0].name)
        assertEquals(FieldType.Score, newTemplate.fields[0].type)
        assertEquals("notes", newTemplate.fields[1].name)
        assertEquals(FieldType.Text, newTemplate.fields[1].type)

        val newEntries = repos.entries.observeByCollection(newId).first()
        assertEquals(2, newEntries.size)

        val byName = newEntries.associateBy { it.location.displayName }
        val paris = byName["Paris"]!!
        assertTrue(abs(48.8566 - paris.location.coordinates.lat) < 1e-9)
        assertTrue(abs(2.3522 - paris.location.coordinates.lng) < 1e-9)
        assertNotNull(paris.review)
        assertEquals("4.5", paris.review!!.data["score"])
        assertEquals("great", paris.review.data["notes"])

        val london = byName["London"]!!
        assertTrue(abs(51.5074 - london.location.coordinates.lat) < 1e-9)
        assertNull(london.review)
    }

    @Test
    fun roundTripGeoJson() = runBlocking {
        val (repos, service) = setup()

        val col = repos.collections.create(
            "Coffee Spots",
            "Best coffee",
            Appearance("#E2553F", "pin"),
        ).getOrThrow()

        repos.templates.define(col.id, listOf(
            TemplateField("rating", "Rating", FieldType.Score, TemplateFieldConfig.Score(), 0),
            TemplateField("vibe", "Vibe", FieldType.Enum, TemplateFieldConfig.Enum(
                kotlinx.collections.immutable.persistentListOf("chill", "lively")
            ), 1),
        )).getOrThrow()

        val loc1 = Location(
            id = LocationId(""),
            coordinates = Coordinates(48.8566, 2.3522),
            displayName = "Café A",
            sourceType = SourceType.Manual,
            sourceId = "cafe-a",
            address = "1 Rue de Rivoli",
            cachedMetadata = null,
            refreshable = false,
        )
        repos.collections.addEntry(
            col.id, loc1,
            ReviewDraft(persistentMapOf("rating" to "4.0", "vibe" to "chill"), templateVersion = 1),
        ).getOrThrow()

        val loc2 = Location(
            id = LocationId(""),
            coordinates = Coordinates(51.5074, -0.1278),
            displayName = "Café B",
            sourceType = SourceType.Manual,
            sourceId = "cafe-b",
            address = null,
            cachedMetadata = null,
            refreshable = false,
        )
        repos.collections.addEntry(col.id, loc2, ReviewDraft(persistentMapOf())).getOrThrow()

        val bundle = service.export(col.id, PortFormat.GeoJson).getOrThrow()
        val newId = service.import(bundle.text, PortFormat.GeoJson).getOrThrow()

        assertTrue(newId != col.id)

        val newColl = repos.collections.observe(newId).first()!!
        assertEquals("Coffee Spots", newColl.name)
        assertEquals("Best coffee", newColl.description)

        val newTemplate = repos.templates.observe(newId).first()!!
        assertEquals(2, newTemplate.fields.size)
        assertEquals("rating", newTemplate.fields[0].name)
        assertEquals(FieldType.Score, newTemplate.fields[0].type)
        assertEquals("vibe", newTemplate.fields[1].name)
        assertEquals(FieldType.Enum, newTemplate.fields[1].type)

        val newEntries = repos.entries.observeByCollection(newId).first()
        assertEquals(2, newEntries.size)

        val byName = newEntries.associateBy { it.location.displayName }
        val cafeA = byName["Café A"]!!
        assertTrue(abs(48.8566 - cafeA.location.coordinates.lat) < 1e-9)
        assertNotNull(cafeA.review)
        assertEquals("4.0", cafeA.review!!.data["rating"])
        assertEquals("chill", cafeA.review.data["vibe"])

        val cafeB = byName["Café B"]!!
        assertNull(cafeB.review)
    }

    @Test
    fun importForeignMinimalGeoJsonFallsBackToWishlist() = runBlocking {
        val (repos, service) = setup()

        val json = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": { "type": "Point", "coordinates": [2.3522, 48.8566] },
                  "properties": {}
                },
                {
                  "type": "Feature",
                  "geometry": { "type": "Point", "coordinates": [-0.1278, 51.5074] },
                  "properties": {}
                }
              ]
            }
        """.trimIndent()

        val newId = service.import(json, PortFormat.GeoJson).getOrThrow()

        val newColl = repos.collections.observe(newId).first()!!
        assertEquals(AppearancePalette.default, newColl.appearance)

        val newTemplate = repos.templates.observe(newId).first()!!
        val wishlistFields = BuiltInTemplates.wishlist.fields
        assertEquals(wishlistFields.size, newTemplate.fields.size)
        newTemplate.fields.forEachIndexed { i, field ->
            assertEquals(wishlistFields[i].name, field.name)
            assertEquals(wishlistFields[i].type, field.type)
        }

        val newEntries = repos.entries.observeByCollection(newId).first()
        assertEquals(2, newEntries.size)
        newEntries.forEach { entry ->
            assertEquals(SourceType.Manual, entry.location.sourceType)
            assertNull(entry.review)
        }
    }

    @Test
    fun importMalformedInputReturnsFailureWithNoPartialCollection() = runBlocking {
        val (repos, service) = setup()

        val beforeCount = repos.collections.observeAll().first().size

        val geoJsonResult = service.import("not json at all {{{", PortFormat.GeoJson)
        assertTrue(geoJsonResult.isFailure)

        val kmlResult = service.import("not xml at all <<<<", PortFormat.Kml)
        assertTrue(kmlResult.isFailure)

        val afterCount = repos.collections.observeAll().first().size
        assertEquals(beforeCount, afterCount)
    }

    @Test
    fun importFiveThousandGeoJsonEntriesReportsProgressAndCompletes() = runBlocking {
        val (repos, service) = setup()
        val entryCount = 5_000
        val json = buildString {
            append("{\"type\":\"FeatureCollection\",\"features\":[")
            repeat(entryCount) { index ->
                if (index > 0) append(',')
                append("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[")
                append(-73.0 + index * 0.00001)
                append(',')
                append(40.0 + index * 0.00001)
                append("]},\"properties\":{\"rtp:displayName\":\"Place ")
                append(index)
                append("\"}}")
            }
            append("]}")
        }
        var finalProgress: ImportProgress? = null

        val id = service.import(json, PortFormat.GeoJson) { finalProgress = it }.getOrThrow()

        assertEquals(entryCount, repos.entries.observeByCollection(id).first().size)
        assertEquals(ImportProgress(entryCount, entryCount), finalProgress)
    }

    @Test
    fun failureHalfwayThroughImportRemovesPartialCollection() = runBlocking {
        val (repos, service) = setup()
        val beforeCount = repos.collections.observeAll().first().size
        val json = """
            {
              "type": "FeatureCollection",
              "features": [
                {"type":"Feature","geometry":{"type":"Point","coordinates":[-73.0,40.0]},"properties":{"rtp:displayName":"First","rtp:sourceType":"Manual","rtp:sourceId":"duplicate"}},
                {"type":"Feature","geometry":{"type":"Point","coordinates":[-73.1,40.1]},"properties":{"rtp:displayName":"Second","rtp:sourceType":"Manual","rtp:sourceId":"duplicate"}}
              ]
            }
        """.trimIndent()

        val result = service.import(json, PortFormat.GeoJson)

        assertTrue(result.isFailure)
        assertEquals(beforeCount, repos.collections.observeAll().first().size)
    }
}
