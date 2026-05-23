package com.saxonthune.ranktheplanet.data.fake

import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.LocationRepository
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.domain.Appearance
import com.saxonthune.ranktheplanet.domain.Collection
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.Entry
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.FieldType
import com.saxonthune.ranktheplanet.domain.TemplateFieldConfig
import com.saxonthune.ranktheplanet.domain.Location
import com.saxonthune.ranktheplanet.domain.LocationId
import com.saxonthune.ranktheplanet.domain.MapOverviewState
import com.saxonthune.ranktheplanet.domain.ReviewDraft
import com.saxonthune.ranktheplanet.domain.ReviewInstance
import com.saxonthune.ranktheplanet.domain.ReviewTemplate
import com.saxonthune.ranktheplanet.domain.SourceType
import com.saxonthune.ranktheplanet.domain.TemplateField
import com.saxonthune.ranktheplanet.domain.Viewport
import com.saxonthune.ranktheplanet.domain.VisiblePin
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.random.Random

private fun newId(): String = Random.nextInt(0x1000000, 0x7fffffff).toString(16)
private const val FAKE_NOW = "2025-05-19T00:00:00Z"

object Fixtures {
    val dripCoffeeId = CollectionId("col-drip")
    val nytTop100Id = CollectionId("col-nyt")
    val geoDiaryId = CollectionId("col-geo")

    val bluebottleLocId = LocationId("loc-bluebottle-mint")
    val perseLocId = LocationId("loc-perse")
    val lumonLocId = LocationId("loc-lumon")
    val goldenGateLocId = LocationId("loc-golden-gate")

    val centralParkLocId = LocationId("loc-central-park")
    val highLineLocId = LocationId("loc-high-line")
    val prospectParkLocId = LocationId("loc-prospect-park")
    val dominoParkLocId = LocationId("loc-domino-park")

    val collections = listOf(
        Collection(
            id = dripCoffeeId,
            name = "Drip Coffee",
            description = "Filter and pour-over spots, ranked.",
            appearance = Appearance(color = "#6F4E37", pinStyle = "circle"),
            templateVersion = 1,
            isVisible = true,
            created = "2024-01-01T10:00:00Z",
            lastModified = "2024-01-01T10:00:00Z"
        ),
        Collection(
            id = nytTop100Id,
            name = "NYT Top 100",
            description = "Tracking the NYT 100 Best Restaurants list.",
            appearance = Appearance(color = "#C0392B", pinStyle = "star"),
            templateVersion = 1,
            isVisible = true,
            created = "2024-02-01T10:00:00Z",
            lastModified = "2024-02-01T10:00:00Z"
        ),
        Collection(
            id = geoDiaryId,
            name = "Geo Diary",
            description = "Places worth remembering, with notes.",
            appearance = Appearance(color = "#2E86AB", pinStyle = "pin"),
            templateVersion = 1,
            isVisible = true,
            created = "2024-03-01T10:00:00Z",
            lastModified = "2024-03-01T10:00:00Z"
        )
    )

    val locations = listOf(
        Location(
            id = bluebottleLocId,
            coordinates = Coordinates(lat = 37.7897, lng = -122.4003),
            displayName = "Blue Bottle Coffee — Mint Plaza",
            sourceType = SourceType.Google,
            sourceId = "ChIJDWqt5kqAhYAR_FpDH7Zvs9E",
            cachedMetadata = null,
            refreshable = true
        ),
        Location(
            id = perseLocId,
            coordinates = Coordinates(lat = 40.7614, lng = -73.9776),
            displayName = "Per Sé",
            sourceType = SourceType.Google,
            sourceId = "ChIJN1t_tDeuEmsRUsoyG83frY4",
            cachedMetadata = null,
            refreshable = true
        ),
        Location(
            id = lumonLocId,
            coordinates = Coordinates(lat = 40.6782, lng = -73.9442),
            displayName = "Lümon",
            sourceType = SourceType.Manual,
            sourceId = "manual-lumon",
            cachedMetadata = null,
            refreshable = false
        ),
        Location(
            id = goldenGateLocId,
            coordinates = Coordinates(lat = 37.8199, lng = -122.4783),
            displayName = "Golden Gate Bridge",
            sourceType = SourceType.Osm,
            sourceId = "osm-75530416",
            cachedMetadata = null,
            refreshable = true
        ),
        Location(
            id = centralParkLocId,
            coordinates = Coordinates(lat = 40.7829, lng = -73.9654),
            displayName = "Central Park — The Mall",
            sourceType = SourceType.Osm,
            sourceId = "osm-4453738",
            cachedMetadata = null,
            refreshable = true
        ),
        Location(
            id = highLineLocId,
            coordinates = Coordinates(lat = 40.7480, lng = -74.0048),
            displayName = "The High Line",
            sourceType = SourceType.Osm,
            sourceId = "osm-4504115",
            cachedMetadata = null,
            refreshable = true
        ),
        Location(
            id = prospectParkLocId,
            coordinates = Coordinates(lat = 40.6602, lng = -73.9690),
            displayName = "Prospect Park — Long Meadow",
            sourceType = SourceType.Osm,
            sourceId = "osm-7773888",
            cachedMetadata = null,
            refreshable = true
        ),
        Location(
            id = dominoParkLocId,
            coordinates = Coordinates(lat = 40.7144, lng = -73.9680),
            displayName = "Domino Park",
            sourceType = SourceType.Osm,
            sourceId = "osm-558129000",
            cachedMetadata = null,
            refreshable = true
        )
    )

    val entries = listOf(
        Entry(
            id = EntryId("ent-bluebottle"),
            collectionId = dripCoffeeId,
            location = locations[0],
            review = ReviewInstance(
                data = persistentMapOf("overall" to "5", "notes" to "Perfect filter, calm atmosphere"),
                recordedTemplateVersion = 1,
                created = "2024-01-15T09:00:00Z",
                lastModified = "2024-01-15T09:00:00Z",
            ),
            added = "2024-01-10T08:00:00Z"
        ),
        Entry(
            id = EntryId("ent-perse"),
            collectionId = nytTop100Id,
            location = locations[1],
            review = ReviewInstance(
                data = persistentMapOf("rating" to "5", "notes" to "Exceptional tasting menu", "visited" to "true"),
                recordedTemplateVersion = 1,
                created = "2024-02-14T20:00:00Z",
                lastModified = "2024-02-14T20:00:00Z",
            ),
            added = "2024-02-01T10:00:00Z"
        ),
        Entry(
            id = EntryId("ent-lumon"),
            collectionId = nytTop100Id,
            location = locations[2],
            review = ReviewInstance(
                data = persistentMapOf("rating" to "4", "notes" to "Creative seasonal menu", "visited" to "true"),
                recordedTemplateVersion = 1,
                created = "2024-03-20T19:00:00Z",
                lastModified = "2024-03-20T19:00:00Z",
            ),
            added = "2024-03-15T14:00:00Z"
        ),
        Entry(
            id = EntryId("ent-geo-central-park"),
            collectionId = geoDiaryId,
            location = locations[4],
            review = ReviewInstance(
                data = persistentMapOf(
                    "description" to "Read on a bench by The Mall while the buskers set up. First real warm afternoon of the year.",
                    "visitedOn" to "2024-04-12"
                ),
                recordedTemplateVersion = 1,
                created = "2024-04-12T15:30:00Z",
                lastModified = "2024-04-12T15:30:00Z",
            ),
            added = "2024-04-12T15:30:00Z"
        ),
        Entry(
            id = EntryId("ent-geo-high-line"),
            collectionId = geoDiaryId,
            location = locations[5],
            review = ReviewInstance(
                data = persistentMapOf(
                    "description" to "Walked the whole High Line at golden hour. Stopped where it overlooks 10th Ave to watch the traffic.",
                    "visitedOn" to "2024-05-03"
                ),
                recordedTemplateVersion = 1,
                created = "2024-05-03T18:45:00Z",
                lastModified = "2024-05-03T18:45:00Z",
            ),
            added = "2024-05-03T18:45:00Z"
        ),
        Entry(
            id = EntryId("ent-geo-prospect-park"),
            collectionId = geoDiaryId,
            location = locations[6],
            review = ReviewInstance(
                data = persistentMapOf(
                    "description" to "Picnic on Long Meadow with friends visiting from out of town. Lost a frisbee in the trees.",
                    "visitedOn" to "2024-06-22"
                ),
                recordedTemplateVersion = 1,
                created = "2024-06-22T13:00:00Z",
                lastModified = "2024-06-22T13:00:00Z",
            ),
            added = "2024-06-22T13:00:00Z"
        ),
        Entry(
            id = EntryId("ent-geo-domino-park"),
            collectionId = geoDiaryId,
            location = locations[7],
            review = ReviewInstance(
                data = persistentMapOf(
                    "description" to "Coffee by the water watching the ferries cross to Manhattan. The old sugar refinery signage still up.",
                    "visitedOn" to "2024-07-09"
                ),
                recordedTemplateVersion = 1,
                created = "2024-07-09T09:30:00Z",
                lastModified = "2024-07-09T09:30:00Z",
            ),
            added = "2024-07-09T09:30:00Z"
        ),
        Entry(
            id = EntryId("ent-geo-golden-gate"),
            collectionId = geoDiaryId,
            location = locations[3],
            review = null,
            added = "2024-08-01T10:00:00Z"
        )
    )

    val templates = listOf(
        ReviewTemplate(
            collectionId = dripCoffeeId,
            version = 1,
            fields = persistentListOf(
                TemplateField(
                    name = "overall", label = "Overall", type = FieldType.Score, required = true,
                    config = TemplateFieldConfig.Score(max = 5.0, render = "stars"), ordinal = 0,
                ),
                TemplateField(
                    name = "notes", label = "Notes", type = FieldType.Text,
                    config = TemplateFieldConfig.Text(multiline = true), ordinal = 1,
                ),
            )
        ),
        ReviewTemplate(
            collectionId = nytTop100Id,
            version = 1,
            fields = persistentListOf(
                TemplateField(
                    name = "rating", label = "Rating", type = FieldType.Score, required = true,
                    config = TemplateFieldConfig.Score(max = 5.0, render = "stars"), ordinal = 0,
                ),
                TemplateField(
                    name = "notes", label = "Notes", type = FieldType.Text,
                    config = TemplateFieldConfig.Text(multiline = true), ordinal = 1,
                ),
                TemplateField(
                    name = "visited", label = "Visited", type = FieldType.Boolean,
                    config = TemplateFieldConfig.BooleanField, ordinal = 2,
                ),
            )
        ),
        ReviewTemplate(
            collectionId = geoDiaryId,
            version = 1,
            fields = persistentListOf(
                TemplateField(
                    name = "description", label = "Description", type = FieldType.Text, required = true,
                    config = TemplateFieldConfig.Text(multiline = true), ordinal = 0,
                ),
                TemplateField(
                    name = "visitedOn", label = "Visited on", type = FieldType.Date,
                    config = TemplateFieldConfig.Date, ordinal = 1,
                ),
            )
        )
    )
}

class InMemoryStore {
    val collections = MutableStateFlow(Fixtures.collections)
    val locations = MutableStateFlow(Fixtures.locations)
    val entries = MutableStateFlow(Fixtures.entries)
    val templates = MutableStateFlow(Fixtures.templates)
}

class FakeCollectionRepository(private val store: InMemoryStore) : CollectionRepository {

    override fun observeAll(): Flow<List<Collection>> = store.collections

    override fun observe(id: CollectionId): Flow<Collection?> =
        store.collections.map { list -> list.find { it.id == id } }

    override suspend fun create(name: String, description: String?, appearance: Appearance): Result<Collection> {
        val collection = Collection(
            id = CollectionId(newId()),
            name = name,
            description = description,
            appearance = appearance,
            templateVersion = 0,
            isVisible = true,
            created = FAKE_NOW,
            lastModified = FAKE_NOW
        )
        store.collections.update { it + collection }
        return Result.success(collection)
    }

    override suspend fun addEntry(
        collectionId: CollectionId,
        location: Location,
        review: ReviewDraft
    ): Result<Entry> {
        val collection = store.collections.value.find { it.id == collectionId }
            ?: return Result.failure(IllegalArgumentException("Collection not found: ${collectionId.value}"))

        if (review.data.isNotEmpty() && review.templateVersion != collection.templateVersion) {
            return Result.failure(IllegalStateException(
                "Template version mismatch: caller=${review.templateVersion} collection=${collection.templateVersion}"
            ))
        }

        if (store.locations.value.none { it.id == location.id }) {
            store.locations.update { it + location }
        }

        val entry = Entry(
            id = EntryId(newId()),
            collectionId = collectionId,
            location = location,
            review = if (review.data.isEmpty()) null else ReviewInstance(
                data = review.data,
                recordedTemplateVersion = review.templateVersion,
                created = FAKE_NOW,
                lastModified = FAKE_NOW,
            ),
            added = FAKE_NOW
        )
        store.entries.update { it + entry }
        return Result.success(entry)
    }

    override suspend fun removeEntry(entryId: EntryId): Result<Unit> {
        store.entries.update { list -> list.filter { it.id != entryId } }
        return Result.success(Unit)
    }
}

class FakeEntryRepository(private val store: InMemoryStore) : EntryRepository {

    override fun observeAll(): Flow<List<Entry>> = store.entries

    override fun observeByCollection(collectionId: CollectionId): Flow<List<Entry>> =
        store.entries.map { list -> list.filter { it.collectionId == collectionId } }

    override fun observe(entryId: EntryId): Flow<Entry?> =
        store.entries.map { list -> list.find { it.id == entryId } }

    override suspend fun editReview(
        entryId: EntryId,
        data: Map<String, String>,
        templateVersion: Int,
    ): Result<Entry> {
        val current = store.entries.value.find { it.id == entryId }
            ?: return Result.failure(IllegalArgumentException("Entry not found: ${entryId.value}"))
        val collection = store.collections.value.find { it.id == current.collectionId }
            ?: return Result.failure(IllegalStateException("Collection not found: ${current.collectionId.value}"))
        if (templateVersion != collection.templateVersion) {
            return Result.failure(IllegalStateException(
                "Template version mismatch: caller=$templateVersion collection=${collection.templateVersion}"
            ))
        }
        val existingReview = current.review
        val updatedReview = if (existingReview != null) {
            existingReview.copy(data = data.toImmutableMap(), lastModified = FAKE_NOW)
        } else {
            ReviewInstance(
                data = data.toImmutableMap(),
                recordedTemplateVersion = templateVersion,
                created = FAKE_NOW,
                lastModified = FAKE_NOW,
            )
        }
        val updated = current.copy(review = updatedReview)
        store.entries.update { list -> list.map { if (it.id == entryId) updated else it } }
        return Result.success(updated)
    }
}

class FakeLocationRepository(private val store: InMemoryStore) : LocationRepository {

    override suspend fun findByIdentity(sourceType: SourceType, sourceId: String): Location? =
        store.locations.value.find { it.sourceType == sourceType && it.sourceId == sourceId }

    override suspend fun upsert(location: Location): Result<Location> {
        val exists = store.locations.value.any { it.id == location.id }
        if (exists) {
            store.locations.update { list -> list.map { if (it.id == location.id) location else it } }
        } else {
            store.locations.update { it + location }
        }
        return Result.success(location)
    }

    override suspend fun merge(keep: LocationId, drop: LocationId): Result<Location> {
        val keepLocation = store.locations.value.find { it.id == keep }
            ?: return Result.failure(IllegalArgumentException("Location not found: ${keep.value}"))

        store.locations.update { list -> list.filter { it.id != drop } }
        store.entries.update { list ->
            list.map { entry ->
                if (entry.location.id == drop) entry.copy(location = keepLocation) else entry
            }
        }
        return Result.success(keepLocation)
    }
}

class FakeTemplateRepository(private val store: InMemoryStore) : TemplateRepository {

    override fun observe(collectionId: CollectionId): Flow<ReviewTemplate?> =
        store.templates.map { list -> list.find { it.collectionId == collectionId } }

    override suspend fun define(
        collectionId: CollectionId,
        fields: List<TemplateField>
    ): Result<ReviewTemplate> {
        val template = ReviewTemplate(
            collectionId = collectionId,
            version = 1,
            fields = fields.toImmutableList()
        )
        store.templates.update { it + template }
        store.collections.update { list ->
            list.map { if (it.id == collectionId) it.copy(templateVersion = 1) else it }
        }
        return Result.success(template)
    }

    override suspend fun edit(
        collectionId: CollectionId,
        fields: List<TemplateField>
    ): Result<ReviewTemplate> {
        val current = store.templates.value.find { it.collectionId == collectionId }
            ?: return Result.failure(IllegalArgumentException("Template not found for: ${collectionId.value}"))

        val updated = current.copy(version = current.version + 1, fields = fields.toImmutableList())
        store.templates.update { list ->
            list.map { if (it.collectionId == collectionId) updated else it }
        }
        store.collections.update { list ->
            list.map { if (it.id == collectionId) it.copy(templateVersion = updated.version) else it }
        }
        return Result.success(updated)
    }
}

class FakeRepositories {
    private val store = InMemoryStore()
    val collections: CollectionRepository = FakeCollectionRepository(store)
    val entries: EntryRepository = FakeEntryRepository(store)
    val locations: LocationRepository = FakeLocationRepository(store)
    val templates: TemplateRepository = FakeTemplateRepository(store)
}
