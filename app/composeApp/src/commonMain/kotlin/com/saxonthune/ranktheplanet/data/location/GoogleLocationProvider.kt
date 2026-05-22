package com.saxonthune.ranktheplanet.data.location

import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.SourceType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val FIELD_MASK =
    "places.id,places.displayName,places.location,places.formattedAddress,places.types"

class GoogleLocationProvider(
    private val client: HttpClient,
    private val apiKey: () -> String?,
    private val endpointBase: String = "https://places.googleapis.com/v1",
) : LocationProvider {

    override val type: SourceType = SourceType.Google

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun resolve(query: String): ProviderResult<List<LocationCandidate>> {
        if (query.isBlank()) return ProviderResult.Ok(emptyList())
        val key = apiKey()
        if (key.isNullOrBlank()) return ProviderResult.Failed(ProviderError.NOT_CONFIGURED)
        return try {
            val response = client.post("$endpointBase/places:searchText") {
                contentType(ContentType.Application.Json)
                header("X-Goog-Api-Key", key)
                header("X-Goog-FieldMask", FIELD_MASK)
                setBody("""{"textQuery":"$query"}""")
            }
            if (!response.status.isSuccess()) {
                return ProviderResult.Failed(ProviderError.PROVIDER_ERROR)
            }
            val result = response.body<GooglePlacesResponse>()
            ProviderResult.Ok(result.places.mapNotNull { it.toCandidate() })
        } catch (e: Exception) {
            ProviderResult.Failed(ProviderError.NETWORK)
        }
    }

    override suspend fun resolveNearby(coordinates: Coordinates): ProviderResult<List<LocationCandidate>> {
        val key = apiKey()
        if (key.isNullOrBlank()) return ProviderResult.Failed(ProviderError.NOT_CONFIGURED)
        return try {
            val body = """{"locationRestriction":{"circle":{"center":{"latitude":${coordinates.lat},"longitude":${coordinates.lng}},"radius":500.0}}}"""
            val response = client.post("$endpointBase/places:searchNearby") {
                contentType(ContentType.Application.Json)
                header("X-Goog-Api-Key", key)
                header("X-Goog-FieldMask", FIELD_MASK)
                setBody(body)
            }
            if (!response.status.isSuccess()) {
                return ProviderResult.Failed(ProviderError.PROVIDER_ERROR)
            }
            val result = response.body<GooglePlacesResponse>()
            ProviderResult.Ok(result.places.mapNotNull { it.toCandidate() })
        } catch (e: Exception) {
            ProviderResult.Failed(ProviderError.NETWORK)
        }
    }

    private fun GooglePlace.toCandidate(): LocationCandidate? {
        val placeId = id ?: return null
        val loc = location ?: return null
        val name = displayName?.text ?: formattedAddress ?: "(unknown)"
        val metadata = GoogleCachedMetadata(
            id = placeId,
            displayName = displayName?.text,
            formattedAddress = formattedAddress,
            types = types,
        )
        return LocationCandidate(
            coordinates = Coordinates(lat = loc.latitude, lng = loc.longitude),
            displayName = name,
            sourceType = SourceType.Google,
            sourceId = placeId,
            cachedMetadata = json.encodeToString(metadata),
        )
    }
}

@Serializable
private data class GoogleCachedMetadata(
    val id: String,
    val displayName: String?,
    val formattedAddress: String?,
    val types: List<String>,
)
