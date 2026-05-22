package com.saxonthune.ranktheplanet.data.location

import com.saxonthune.ranktheplanet.domain.SourceType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class GoogleLocationProviderTest {

    private fun mockClient(
        responseBody: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpClient {
        val engine = MockEngine {
            respond(
                content = responseBody,
                status = status,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }

    private fun throwingClient(): HttpClient {
        val engine = MockEngine { error("HTTP engine must not be invoked") }
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }

    @Test
    fun `resolve maps searchText response to LocationCandidates`() = runBlocking {
        val placesJson = """
            {
              "places": [
                {
                  "id": "places/ChIJN1t_tDeuEmsRUsoyG83frY4",
                  "displayName": {"text": "Google Sydney", "languageCode": "en"},
                  "location": {"latitude": -33.8688, "longitude": 151.2093},
                  "formattedAddress": "48 Pirrama Rd, Pyrmont NSW 2009, Australia",
                  "types": ["establishment", "point_of_interest"]
                }
              ]
            }
        """.trimIndent()

        val provider = GoogleLocationProvider(
            client = mockClient(placesJson),
            apiKey = { "test-key" },
        )
        val result = provider.resolve("Google Sydney")

        assertIs<ProviderResult.Ok<List<LocationCandidate>>>(result)
        val candidates = result.value
        assertEquals(1, candidates.size)
        val c = candidates[0]
        assertEquals(-33.8688, c.coordinates.lat)
        assertEquals(151.2093, c.coordinates.lng)
        assertEquals(SourceType.Google, c.sourceType)
        assertEquals("places/ChIJN1t_tDeuEmsRUsoyG83frY4", c.sourceId)
        assertEquals("Google Sydney", c.displayName)
    }

    @Test
    fun `resolve returns Failed on 401 response`() = runBlocking {
        val provider = GoogleLocationProvider(
            client = mockClient("{}", status = HttpStatusCode.Unauthorized),
            apiKey = { "test-key" },
        )
        val result = provider.resolve("anywhere")
        assertIs<ProviderResult.Failed>(result)
        assertEquals(ProviderError.PROVIDER_ERROR, result.error)
    }

    @Test
    fun `resolve returns NOT_CONFIGURED when apiKey is null without invoking HTTP engine`() = runBlocking {
        val provider = GoogleLocationProvider(
            client = throwingClient(),
            apiKey = { null },
        )
        val result = provider.resolve("anywhere")
        assertIs<ProviderResult.Failed>(result)
        assertEquals(ProviderError.NOT_CONFIGURED, result.error)
    }

    @Test
    fun `cachedMetadata contains only minimal fields not the full response`() = runBlocking {
        val placesJson = """
            {
              "places": [
                {
                  "id": "places/abc123",
                  "displayName": {"text": "Test Place", "languageCode": "en"},
                  "location": {"latitude": 1.0, "longitude": 2.0},
                  "formattedAddress": "1 Test St",
                  "types": ["restaurant"]
                }
              ]
            }
        """.trimIndent()

        val provider = GoogleLocationProvider(
            client = mockClient(placesJson),
            apiKey = { "test-key" },
        )
        val result = provider.resolve("Test Place")
        assertIs<ProviderResult.Ok<List<LocationCandidate>>>(result)
        val metadata = result.value[0].cachedMetadata
        assertNotNull(metadata)

        val parsed = Json.parseToJsonElement(metadata).jsonObject
        assertEquals("places/abc123", parsed["id"]?.jsonPrimitive?.content)
        assertEquals("Test Place", parsed["displayName"]?.jsonPrimitive?.content)
        assertEquals("1 Test St", parsed["formattedAddress"]?.jsonPrimitive?.content)
        assertFalse(parsed.containsKey("languageCode"), "raw DTO field languageCode must not appear in cachedMetadata")
        assertFalse(parsed.containsKey("location"), "raw location object must not appear in cachedMetadata")
    }
}
