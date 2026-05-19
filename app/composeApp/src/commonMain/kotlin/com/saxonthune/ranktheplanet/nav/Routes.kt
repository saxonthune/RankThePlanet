package com.saxonthune.ranktheplanet.nav

import kotlinx.serialization.Serializable

@Serializable object MapOverview
@Serializable object CollectionList
@Serializable data class CollectionDetail(val collectionId: String)
@Serializable data class CollectionEntryDetail(val entryId: String)
@Serializable object ReviewForm
@Serializable object CollectionEditor
@Serializable object LocationPicker
@Serializable object AddLocationToCollection
@Serializable object LocationDraft
@Serializable object ImportFlow
@Serializable object Settings
