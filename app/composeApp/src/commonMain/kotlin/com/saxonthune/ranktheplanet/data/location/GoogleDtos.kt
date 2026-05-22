package com.saxonthune.ranktheplanet.data.location

import kotlinx.serialization.Serializable

@Serializable
data class GooglePlacesResponse(val places: List<GooglePlace> = emptyList())

@Serializable
data class GooglePlace(
    val id: String? = null,
    val displayName: GoogleLocalizedText? = null,
    val location: GoogleLatLng? = null,
    val formattedAddress: String? = null,
    val types: List<String> = emptyList(),
)

@Serializable
data class GoogleLocalizedText(val text: String? = null, val languageCode: String? = null)

@Serializable
data class GoogleLatLng(val latitude: Double = 0.0, val longitude: Double = 0.0)
