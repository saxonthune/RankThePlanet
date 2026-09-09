package com.saxonthune.ranktheplanet.domain.io

import com.saxonthune.ranktheplanet.domain.Appearance
import com.saxonthune.ranktheplanet.domain.Collection
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.Entry
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.FieldType
import com.saxonthune.ranktheplanet.domain.Location
import com.saxonthune.ranktheplanet.domain.LocationId
import com.saxonthune.ranktheplanet.domain.ReviewInstance
import com.saxonthune.ranktheplanet.domain.ReviewTemplate
import com.saxonthune.ranktheplanet.domain.SourceType
import com.saxonthune.ranktheplanet.domain.TemplateField
import com.saxonthune.ranktheplanet.domain.TemplateFieldConfig
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KmlCodecTest {

    private val testCollection = Collection(
        id = CollectionId("c1"),
        name = "My Places",
        description = "A test collection",
        appearance = Appearance(color = "#E2553F", pinStyle = "pin"),
        templateVersion = 2,
        isVisible = true,
        powerRanking = true,
        created = "2024-01-01T00:00:00Z",
        lastModified = "2024-06-01T00:00:00Z",
    )

    private val testTemplate = ReviewTemplate(
        collectionId = CollectionId("c1"),
        version = 2,
        fields = persistentListOf(
            TemplateField("score", "Score", FieldType.Score, TemplateFieldConfig.Score(0.0, 10.0, 1.0, "stars"), 0),
            TemplateField("notes", "Notes", FieldType.Text, TemplateFieldConfig.TextField, 1),
            TemplateField("vibe", "Vibe", FieldType.Enum, TemplateFieldConfig.Enum(persistentListOf("chill", "lively")), 2),
            TemplateField("visited", "Visited", FieldType.Boolean, TemplateFieldConfig.BooleanField, 3),
            TemplateField("visitDate", "Visit Date", FieldType.Date, TemplateFieldConfig.Date, 4),
        ),
        summaryField = "score",
    )

    private fun makeLocation(id: String, sourceType: SourceType, withAddress: Boolean = true) = Location(
        id = LocationId(id),
        coordinates = Coordinates(lat = 48.8566, lng = 2.3522),
        displayName = "Place $id",
        sourceType = sourceType,
        sourceId = "src-$id",
        address = if (withAddress) "123 Main St" else null,
        cachedMetadata = null,
        refreshable = false,
    )

    private fun makeEntry(id: String, location: Location, withReview: Boolean): Entry {
        val review = if (withReview) ReviewInstance(
            data = persistentMapOf("score" to "8", "notes" to "great"),
            recordedTemplateVersion = 2,
            created = "2024-03-01T00:00:00Z",
            lastModified = "2024-03-02T00:00:00Z",
        ) else null
        return Entry(
            id = EntryId(id),
            collectionId = CollectionId("c1"),
            location = location,
            review = review,
            added = "2024-02-01T00:00:00Z",
        )
    }

    @Test
    fun roundTripPreservesCollectionMetadata() {
        val entries = listOf(makeEntry("e1", makeLocation("l1", SourceType.Google), withReview = true))
        val kml = KmlCodec.encode(testCollection, testTemplate, entries)
        val result = KmlCodec.decode(kml)
        assertTrue(result.isSuccess)
        val portable = result.getOrThrow()
        assertEquals("My Places", portable.collection.name)
        assertEquals("A test collection", portable.collection.description)
        assertEquals("#E2553F", portable.collection.appearance.color)
        assertEquals("pin", portable.collection.appearance.pinStyle)
        assertEquals(true, portable.collection.powerRanking)
        assertEquals("2024-01-01T00:00:00Z", portable.collection.created)
        assertEquals("2024-06-01T00:00:00Z", portable.collection.lastModified)
    }

    @Test
    fun roundTripPreservesAllTemplateFieldConfigVariants() {
        val entries = listOf(makeEntry("e1", makeLocation("l1", SourceType.Google), withReview = false))
        val kml = KmlCodec.encode(testCollection, testTemplate, entries)
        val result = KmlCodec.decode(kml)
        assertTrue(result.isSuccess)
        val template = result.getOrThrow().template!!
        assertEquals(2, template.version)
        assertEquals("score", template.summaryField)
        assertEquals(5, template.fields.size)

        val score = template.fields[0].config as TemplateFieldConfig.Score
        assertEquals(0.0, score.min)
        assertEquals(10.0, score.max)
        assertEquals(1.0, score.step)
        assertEquals("stars", score.render)

        assertTrue(template.fields[1].config is TemplateFieldConfig.TextField)

        val enum = template.fields[2].config as TemplateFieldConfig.Enum
        assertEquals(listOf("chill", "lively"), enum.options.toList())

        assertTrue(template.fields[3].config is TemplateFieldConfig.BooleanField)
        assertTrue(template.fields[4].config is TemplateFieldConfig.Date)
    }

    @Test
    fun roundTripEntryWithReview() {
        val entries = listOf(makeEntry("e1", makeLocation("l1", SourceType.Google), withReview = true))
        val kml = KmlCodec.encode(testCollection, testTemplate, entries)
        val result = KmlCodec.decode(kml)
        assertTrue(result.isSuccess)
        val entry = result.getOrThrow().entries[0]
        val review = entry.review!!
        assertEquals(2, review.recordedTemplateVersion)
        assertEquals("8", review.data["score"])
        assertEquals("great", review.data["notes"])
    }

    @Test
    fun roundTripEntryWithoutReview() {
        val entries = listOf(makeEntry("e1", makeLocation("l1", SourceType.Manual), withReview = false))
        val kml = KmlCodec.encode(testCollection, testTemplate, entries)
        val result = KmlCodec.decode(kml)
        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow().entries[0].review)
    }

    @Test
    fun roundTripAllSourceTypes() {
        val entries = SourceType.entries.mapIndexed { i, st ->
            makeEntry("e$i", makeLocation("l$i", st), withReview = false)
        }
        val kml = KmlCodec.encode(testCollection, testTemplate, entries)
        val result = KmlCodec.decode(kml)
        assertTrue(result.isSuccess)
        val decoded = result.getOrThrow().entries
        SourceType.entries.forEachIndexed { i, st ->
            assertEquals(st, decoded[i].location.sourceType)
        }
    }

    @Test
    fun roundTripAddressNullVsPresent() {
        val withAddr = makeEntry("e1", makeLocation("l1", SourceType.Manual, withAddress = true), false)
        val withoutAddr = makeEntry("e2", makeLocation("l2", SourceType.Manual, withAddress = false), false)
        val kml = KmlCodec.encode(testCollection, null, listOf(withAddr, withoutAddr))
        val result = KmlCodec.decode(kml)
        assertTrue(result.isSuccess)
        assertEquals("123 Main St", result.getOrThrow().entries[0].location.address)
        assertNull(result.getOrThrow().entries[1].location.address)
    }

    @Test
    fun xmlEscapingRoundTrips() {
        val special = Collection(
            id = CollectionId("c1"),
            name = """Caf&#xe9; & "Bistro" <Paris> 'Today'""",
            description = "A & B < C > D",
            appearance = Appearance(color = "#E2553F", pinStyle = "pin"),
            templateVersion = 0,
            isVisible = true,
            powerRanking = false,
            created = "2024-01-01T00:00:00Z",
            lastModified = "2024-01-01T00:00:00Z",
        )
        val specialName = """Caf&#xe9; & "Bistro" <Paris> 'Today'"""
        val specialCollection = special.copy(name = specialName)
        val kml = KmlCodec.encode(specialCollection, null, emptyList())
        val result = KmlCodec.decode(kml)
        assertTrue(result.isSuccess)
        assertEquals(specialName, result.getOrThrow().collection.name)
    }

    @Test
    fun decodeMinimalForeignKml() {
        val kml = """
            <kml>
              <Document>
                <Placemark>
                  <name>X</name>
                  <Point><coordinates>10,20</coordinates></Point>
                </Placemark>
              </Document>
            </kml>
        """.trimIndent()
        val result = KmlCodec.decode(kml)
        assertTrue(result.isSuccess)
        val portable = result.getOrThrow()
        assertNull(portable.template)
        assertEquals(1, portable.entries.size)
        val entry = portable.entries[0]
        assertEquals("X", entry.location.displayName)
        assertEquals(20.0, entry.location.coordinates.lat)
        assertEquals(10.0, entry.location.coordinates.lng)
        assertEquals(SourceType.Manual, entry.location.sourceType)
        assertNull(entry.review)
    }

    @Test
    fun decodeMalformedCoordinatesReturnsFailure() {
        val kml = """
            <kml>
              <Document>
                <Placemark>
                  <name>X</name>
                  <Point><coordinates>notanumber,also</coordinates></Point>
                </Placemark>
              </Document>
            </kml>
        """.trimIndent()
        val result = KmlCodec.decode(kml)
        assertTrue(result.isFailure)
    }

    @Test
    fun decodeNonPointGeometryReturnsFailure() {
        val kml = """
            <kml>
              <Document>
                <Placemark>
                  <name>X</name>
                  <LineString><coordinates>0,0 1,1</coordinates></LineString>
                </Placemark>
              </Document>
            </kml>
        """.trimIndent()
        val result = KmlCodec.decode(kml)
        assertTrue(result.isFailure)
    }
}
