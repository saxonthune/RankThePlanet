package com.saxonthune.ranktheplanet.nav

import com.saxonthune.ranktheplanet.domain.CollectionId
import kotlinx.serialization.Serializable

@Serializable data class MapOverview(val addToCollectionId: String? = null)
@Serializable object CollectionList
@Serializable data class CollectionDetail(val collectionId: String)
@Serializable data class CollectionEntryDetail(val entryId: String)
@Serializable data class ReviewForm(val entryId: String)
@Serializable object CollectionEditor
@Serializable object ImportFlow
@Serializable object Settings
@Serializable object ManageProviders
@Serializable data class ProviderConfig(val provider: String)

sealed interface MapMode {
    data object Browse : MapMode
    data class AddingToCollection(
        val collectionId: CollectionId,
        val collectionName: String,
    ) : MapMode
}
