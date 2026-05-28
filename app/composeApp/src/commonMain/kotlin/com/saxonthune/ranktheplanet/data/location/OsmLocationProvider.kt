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
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class OsmLocationProvider(
    private val client: HttpClient,
    private val photonBase: String = "https://photon.komoot.io",
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

    override suspend fun resolve(query: String, bias: LocationBias?): ProviderResult<List<LocationCandidate>> {
        throttle()
        return try {
            val response = client.get("$photonBase/api") {
                parameter("q", query)
                parameter("limit", "10")
                when (bias) {
                    is LocationBias.Point -> {
                        parameter("lat", bias.coordinates.lat.toString())
                        parameter("lon", bias.coordinates.lng.toString())
                    }
                    is LocationBias.Box -> {
                        parameter("bbox", "${bias.west},${bias.south},${bias.east},${bias.north}")
                    }
                    null -> Unit
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
            val response = client.get("$photonBase/reverse") {
                parameter("lat", coordinates.lat.toString())
                parameter("lon", coordinates.lng.toString())
                parameter("limit", "10")
            }
            if (!response.status.isSuccess()) {
                return ProviderResult.Failed(ProviderError.PROVIDER_ERROR)
            }
            val collection = response.body<PhotonFeatureCollection>()
            val candidates = collection.features
                .map { it.toCandidate() }
                .map { it.withDistanceDetail(coordinates) }
                .sortedBy { distanceMeters(coordinates, it.coordinates) }
            ProviderResult.Ok(candidates)
        } catch (e: Exception) {
            ProviderResult.Failed(ProviderError.NETWORK)
        }
    }

    private fun LocationCandidate.withDistanceDetail(from: Coordinates): LocationCandidate {
        val formatted = formatDistance(distanceMeters(from, coordinates))
        val combined = listOfNotNull(formatted, detail).joinToString(" · ").takeIf { it.isNotBlank() }
        return copy(detail = combined)
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
        val streetLine = listOfNotNull(properties.housenumber, properties.street)
            .joinToString(" ")
            .takeIf { it.isNotBlank() }
        val primary = properties.name ?: streetLine ?: properties.city ?: "(unknown)"
        val detailParts = buildList {
            if (properties.name != null && streetLine != null) add(streetLine)
            properties.city?.let { add(it) }
            properties.state?.let { add(it) }
            properties.country?.let { add(it) }
        }.filterNot { it == primary }
        val detail = detailParts.joinToString(", ").takeIf { it.isNotBlank() }
        val sourceId = buildSourceId(properties.osm_type, properties.osm_id)
        return LocationCandidate(
            coordinates = Coordinates(lat = lat, lng = lng),
            displayName = primary,
            sourceType = SourceType.Osm,
            sourceId = sourceId,
            cachedMetadata = json.encodeToString(this),
            detail = detail,
        )
    }

    private fun buildSourceId(osmType: String?, osmId: Long?): String? {
        if (osmId == null) return null
        val initial = osmType?.firstOrNull()?.uppercaseChar() ?: return null
        return "$initial$osmId"
    }
}

private const val EARTH_RADIUS_M = 6_371_000.0

private fun distanceMeters(a: Coordinates, b: Coordinates): Double {
    val toRad = PI / 180.0
    val dLat = (b.lat - a.lat) * toRad
    val dLng = (b.lng - a.lng) * toRad
    val lat1 = a.lat * toRad
    val lat2 = b.lat * toRad
    val h = sin(dLat / 2).let { it * it } +
        cos(lat1) * cos(lat2) * sin(dLng / 2).let { it * it }
    return 2 * EARTH_RADIUS_M * asin(sqrt(h))
}

private fun formatDistance(meters: Double): String =
    if (meters < 1000.0) "${meters.roundToInt()} m"
    else {
        val km = (meters / 100.0).roundToInt() / 10.0
        "$km km"
    }
