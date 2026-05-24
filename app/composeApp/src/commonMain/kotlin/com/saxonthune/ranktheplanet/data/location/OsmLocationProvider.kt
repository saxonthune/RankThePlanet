package com.saxonthune.ranktheplanet.data.location

import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.SourceType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class OsmLocationProvider(
    private val client: HttpClient,
    private val photonBase: String = "https://photon.komoot.io",
    private val nominatimBase: String = "https://nominatim.openstreetmap.org"
) : LocationProvider {

    override val type: SourceType = SourceType.Osm
    override val supportsTypeahead: Boolean = true

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val throttleMutex = Mutex()
    private val timeSource = TimeSource.Monotonic
    private var lastRequestMark: TimeSource.Monotonic.ValueTimeMark? = null

    private suspend fun throttle() {
        throttleMutex.withLock {
            val mark = lastRequestMark
            if (mark != null) {
                val elapsed = mark.elapsedNow()
                if (elapsed < 1000.milliseconds) {
                    delay((1000.milliseconds - elapsed).inWholeMilliseconds)
                }
            }
            lastRequestMark = timeSource.markNow()
        }
    }

    override suspend fun resolve(query: String, near: Coordinates?): ProviderResult<List<LocationCandidate>> {
        throttle()
        return try {
            val response = client.get("$photonBase/api") {
                parameter("q", query)
                parameter("limit", "10")
                if (near != null) {
                    parameter("lat", near.lat.toString())
                    parameter("lon", near.lng.toString())
                }
            }
            if (!response.status.isSuccess()) {
                return ProviderResult.Failed(ProviderError.PROVIDER_ERROR)
            }
            val collection = response.body<PhotonFeatureCollection>()
            ProviderResult.Ok(collection.features.map { it.toCandidate() })
        } catch (e: Exception) {
            ProviderResult.Failed(ProviderError.NETWORK)
        }
    }

    override suspend fun resolveNearby(coordinates: Coordinates): ProviderResult<List<LocationCandidate>> {
        throttle()
        return try {
            val response = client.get("$nominatimBase/reverse") {
                parameter("lat", coordinates.lat.toString())
                parameter("lon", coordinates.lng.toString())
                parameter("format", "jsonv2")
            }
            if (!response.status.isSuccess()) {
                return ProviderResult.Failed(ProviderError.PROVIDER_ERROR)
            }
            val place = response.body<NominatimPlace>()
            ProviderResult.Ok(listOf(place.toCandidate()))
        } catch (e: Exception) {
            ProviderResult.Failed(ProviderError.NETWORK)
        }
    }

    override suspend fun healthCheck(): ProviderResult<Unit> {
        throttle()
        return try {
            val response = client.get("$photonBase/api") {
                parameter("q", "test")
                parameter("limit", "1")
            }
            when {
                response.status.isSuccess() -> ProviderResult.Ok(Unit)
                response.status.value == 429 -> ProviderResult.Failed(ProviderError.RATE_LIMITED)
                else -> ProviderResult.Failed(ProviderError.PROVIDER_ERROR)
            }
        } catch (e: Exception) {
            ProviderResult.Failed(ProviderError.NETWORK)
        }
    }

    private fun PhotonFeature.toCandidate(): LocationCandidate {
        val lng = geometry.coordinates.getOrElse(0) { 0.0 }
        val lat = geometry.coordinates.getOrElse(1) { 0.0 }
        val nameParts = listOfNotNull(
            properties.name,
            properties.city,
            properties.state,
            properties.country
        )
        val displayName = if (nameParts.isEmpty()) "(unknown)" else nameParts.joinToString(", ")
        val sourceId = buildSourceId(properties.osm_type, properties.osm_id)
        return LocationCandidate(
            coordinates = Coordinates(lat = lat, lng = lng),
            displayName = displayName,
            sourceType = SourceType.Osm,
            sourceId = sourceId,
            cachedMetadata = json.encodeToString(this)
        )
    }

    private fun NominatimPlace.toCandidate(): LocationCandidate {
        val sourceId = buildSourceId(osm_type, osm_id)
        return LocationCandidate(
            coordinates = Coordinates(lat = lat.toDouble(), lng = lon.toDouble()),
            displayName = displayName,
            sourceType = SourceType.Osm,
            sourceId = sourceId,
            cachedMetadata = json.encodeToString(this)
        )
    }

    private fun buildSourceId(osmType: String?, osmId: Long?): String? {
        if (osmId == null) return null
        val initial = osmType?.firstOrNull()?.uppercaseChar() ?: return null
        return "$initial$osmId"
    }
}
