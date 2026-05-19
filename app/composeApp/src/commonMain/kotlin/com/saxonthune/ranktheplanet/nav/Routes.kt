package com.saxonthune.ranktheplanet.nav

import com.saxonthune.ranktheplanet.data.fake.Fixtures
import kotlinx.serialization.Serializable

@Serializable object MapOverview
@Serializable object CollectionList
@Serializable data class CollectionDetail(val collectionId: String)
@Serializable data class CollectionEntryDetail(val entryId: String)
@Serializable object ReviewForm
@Serializable object SchemaBuilder
@Serializable object LocationPicker
@Serializable object AddLocationToCollection
@Serializable object LocationDraft
@Serializable object ImportFlow
@Serializable object Settings

fun Screen.toRoute(): Any = when (this) {
    Screen.MapOverview -> MapOverview
    Screen.CollectionList -> CollectionList
    Screen.CollectionDetail -> CollectionDetail(Fixtures.dripCoffeeId.value)
    Screen.CollectionEntryDetail -> CollectionEntryDetail("ent-bluebottle")
    Screen.ReviewForm -> ReviewForm
    Screen.SchemaBuilder -> SchemaBuilder
    Screen.LocationPicker -> LocationPicker
    Screen.AddLocationToCollection -> AddLocationToCollection
    Screen.LocationDraft -> LocationDraft
    Screen.ImportFlow -> ImportFlow
    Screen.Settings -> Settings
}
