package com.saxonthune.ranktheplanet.data.location

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PhotonFeatureCollection(
    val features: List<PhotonFeature> = emptyList()
)

@Serializable
internal data class PhotonFeature(
    val geometry: PhotonGeometry,
    val properties: PhotonProperties
)

@Serializable
internal data class PhotonGeometry(
    val coordinates: List<Double>  // [lng, lat]
)

@Serializable
internal data class PhotonProperties(
    val osm_id: Long? = null,
    val osm_type: String? = null,
    val name: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val postcode: String? = null,
    val county: String? = null
)

@Serializable
internal data class NominatimPlace(
    val lat: String,
    val lon: String,
    @SerialName("display_name") val displayName: String,
    val osm_id: Long? = null,
    val osm_type: String? = null
)
