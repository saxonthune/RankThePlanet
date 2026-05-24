package com.saxonthune.ranktheplanet.data.location

import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.SourceType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

private const val DETAILS_FIELD_MASK =
    "id,displayName,location,formattedAddress,types"
private const val NEARBY_FIELD_MASK =
    "places.id,places.displayName,places.location,places.formattedAddress,places.types"

class GoogleLocationProvider(
    private val client: HttpClient,
    private val apiKey: () -> String?,
    private val endpointBase: String = "https://places.googleapis.com/v1",
) : LocationProvider {

    override val type: SourceType = SourceType.Google
    override val supportsTypeahead: Boolean = true

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val sessionMutex = Mutex()
    private var currentSessionToken: String? = null

    private suspend fun sessionToken(): String = sessionMutex.withLock {
        currentSessionToken ?: newToken().also { currentSessionToken = it }
    }

    private suspend fun rotateSession() = sessionMutex.withLock {
        currentSessionToken = null
    }

    private fun newToken(): String =
        (1..32).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")

    override suspend fun resolve(query: String, near: Coordinates?): ProviderResult<List<LocationCandidate>> {
        if (query.isBlank()) return ProviderResult.Ok(emptyList())
        val key = apiKey()
        if (key.isNullOrBlank()) return ProviderResult.Failed(ProviderError.NOT_CONFIGURED)
        return try {
            val token = sessionToken()
            val biasFragment = near?.let {
                ""","locationBias":{"circle":{"center":{"latitude":${it.lat},"longitude":${it.lng}},"radius":50000.0}}"""
            } ?: ""
            val body = """{"input":"$query","sessionToken":"$token"$biasFragment}"""
            val response = client.post("$endpointBase/places:autocomplete") {
                contentType(ContentType.Application.Json)
                header("X-Goog-Api-Key", key)
                setBody(body)
            }
            if (!response.status.isSuccess()) {
                return ProviderResult.Failed(ProviderError.PROVIDER_ERROR)
            }
            val result = response.body<GoogleAutocompleteResponse>()
            ProviderResult.Ok(result.suggestions.mapNotNull { it.toCandidate() })
        } catch (e: Exception) {
            ProviderResult.Failed(ProviderError.NETWORK)
        }
    }

    override suspend fun confirm(candidate: LocationCandidate): ProviderResult<LocationCandidate> {
        if (!candidate.needsConfirmation) return ProviderResult.Ok(candidate)
        val placeId = candidate.sourceId
            ?: return ProviderResult.Failed(ProviderError.PROVIDER_ERROR)
        val key = apiKey()
        if (key.isNullOrBlank()) return ProviderResult.Failed(ProviderError.NOT_CONFIGURED)
        return try {
            val token = sessionToken()
            val response = client.get("$endpointBase/places/$placeId") {
                header("X-Goog-Api-Key", key)
                header("X-Goog-FieldMask", DETAILS_FIELD_MASK)
                parameter("sessionToken", token)
            }
            if (!response.status.isSuccess()) {
                return ProviderResult.Failed(ProviderError.PROVIDER_ERROR)
            }
            val place = response.body<GooglePlace>()
            rotateSession()
            val loc = place.location ?: return ProviderResult.Failed(ProviderError.PROVIDER_ERROR)
            val name = place.displayName?.text ?: place.formattedAddress ?: candidate.displayName
            val metadata = GoogleCachedMetadata(
                id = place.id ?: placeId,
                displayName = place.displayName?.text,
                formattedAddress = place.formattedAddress,
                types = place.types,
            )
            ProviderResult.Ok(
                candidate.copy(
                    coordinates = Coordinates(loc.latitude, loc.longitude),
                    displayName = name,
                    cachedMetadata = json.encodeToString(metadata),
                    detail = place.formattedAddress?.takeIf { it.isNotBlank() && it != name },
                    needsConfirmation = false,
                )
            )
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
                header("X-Goog-FieldMask", NEARBY_FIELD_MASK)
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

    override suspend fun healthCheck(): ProviderResult<Unit> {
        val key = apiKey()
        if (key.isNullOrBlank()) return ProviderResult.Failed(ProviderError.NOT_CONFIGURED)
        return try {
            val response = client.post("$endpointBase/places:autocomplete") {
                contentType(ContentType.Application.Json)
                header("X-Goog-Api-Key", key)
                setBody("""{"input":"a"}""")
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

    private fun GoogleAutocompleteSuggestion.toCandidate(): LocationCandidate? {
        val pred = placePrediction ?: return null
        val placeId = pred.placeId ?: return null
        val main = pred.structuredFormat?.mainText?.text ?: pred.text?.text ?: return null
        val secondary = pred.structuredFormat?.secondaryText?.text
        return LocationCandidate(
            coordinates = Coordinates(0.0, 0.0),
            displayName = main,
            sourceType = SourceType.Google,
            sourceId = placeId,
            cachedMetadata = null,
            detail = secondary,
            needsConfirmation = true,
        )
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
        val detail = formattedAddress?.takeIf { it.isNotBlank() && it != name }
        return LocationCandidate(
            coordinates = Coordinates(lat = loc.latitude, lng = loc.longitude),
            displayName = name,
            sourceType = SourceType.Google,
            sourceId = placeId,
            cachedMetadata = json.encodeToString(metadata),
            detail = detail,
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
