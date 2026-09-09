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
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
    fun `resolve maps autocomplete predictions to unconfirmed candidates`() = runBlocking {
        // resolve() hits places:autocomplete, which returns lightweight predictions —
        // a placeId and labels, but no coordinates. The candidate is unconfirmed until
        // confirm() fetches place details (see the confirm test below).
        val autocompleteJson = """
            {
              "suggestions": [
                {
                  "placePrediction": {
                    "placeId": "ChIJN1t_tDeuEmsRUsoyG83frY4",
                    "structuredFormat": {
                      "mainText": {"text": "Google Sydney"},
                      "secondaryText": {"text": "48 Pirrama Rd, Pyrmont NSW 2009, Australia"}
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        val provider = GoogleLocationProvider(
            client = mockClient(autocompleteJson),
            apiKey = { "test-key" },
        )
        val result = provider.resolve("Google Sydney")

        assertIs<ProviderResult.Ok<List<LocationCandidate>>>(result)
        val candidates = result.value
        assertEquals(1, candidates.size)
        val c = candidates[0]
        assertEquals(0.0, c.coordinates.lat)
        assertEquals(0.0, c.coordinates.lng)
        assertTrue(c.needsConfirmation)
        assertEquals(SourceType.Google, c.sourceType)
        assertEquals("ChIJN1t_tDeuEmsRUsoyG83frY4", c.sourceId)
        assertEquals("Google Sydney", c.displayName)
        assertEquals("48 Pirrama Rd, Pyrmont NSW 2009, Australia", c.detail)
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
    fun `resolve with near includes locationBias circle in request body`() = runBlocking {
        val placesJson = """{"places":[]}"""
        var capturedBody: String? = null
        val engine = MockEngine { request ->
            capturedBody = (request.body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString()
                ?: (request.body as? TextContent)?.text
            respond(
                content = placesJson,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        val provider = GoogleLocationProvider(
            client = client,
            apiKey = { "test-key" },
        )
        provider.resolve(
            "coffee",
            bias = LocationBias.Point(com.saxonthune.ranktheplanet.domain.Coordinates(lat = 37.7749, lng = -122.4194)),
        )

        assertNotNull(capturedBody)
        val parsed = Json.parseToJsonElement(capturedBody!!).jsonObject
        val bias = parsed["locationBias"]?.jsonObject
        assertNotNull(bias, "locationBias must be present in request body")
        val circle = bias["circle"]?.jsonObject
        assertNotNull(circle, "locationBias.circle must be present")
        val center = circle["center"]?.jsonObject
        assertNotNull(center, "locationBias.circle.center must be present")
        assertEquals(37.7749, center["latitude"]?.jsonPrimitive?.double)
        assertEquals(-122.4194, center["longitude"]?.jsonPrimitive?.double)
        assertEquals(50000.0, circle["radius"]?.jsonPrimitive?.double)
    }

    @Test
    fun `resolve with box bias includes locationBias rectangle in request body`() = runBlocking {
        val placesJson = """{"places":[]}"""
        var capturedBody: String? = null
        val engine = MockEngine { request ->
            capturedBody = (request.body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString()
                ?: (request.body as? TextContent)?.text
            respond(
                content = placesJson,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        val provider = GoogleLocationProvider(
            client = client,
            apiKey = { "test-key" },
        )
        provider.resolve(
            "cinema",
            bias = LocationBias.Box(south = 40.70, west = -74.02, north = 40.80, east = -73.93),
        )

        assertNotNull(capturedBody)
        val parsed = Json.parseToJsonElement(capturedBody!!).jsonObject
        val bias = parsed["locationBias"]?.jsonObject
        assertNotNull(bias, "locationBias must be present in request body")
        val rect = bias["rectangle"]?.jsonObject
        assertNotNull(rect, "locationBias.rectangle must be present")
        val low = rect["low"]?.jsonObject
        val high = rect["high"]?.jsonObject
        assertNotNull(low); assertNotNull(high)
        assertEquals(40.70, low["latitude"]?.jsonPrimitive?.double)
        assertEquals(-74.02, low["longitude"]?.jsonPrimitive?.double)
        assertEquals(40.80, high["latitude"]?.jsonPrimitive?.double)
        assertEquals(-73.93, high["longitude"]?.jsonPrimitive?.double)
    }

    @Test
    fun `resolve without near does not include locationBias in request body`() = runBlocking {
        val placesJson = """{"places":[]}"""
        var capturedBody: String? = null
        val engine = MockEngine { request ->
            capturedBody = (request.body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString()
                ?: (request.body as? TextContent)?.text
            respond(
                content = placesJson,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        val provider = GoogleLocationProvider(
            client = client,
            apiKey = { "test-key" },
        )
        provider.resolve("coffee")

        assertNotNull(capturedBody)
        val parsed = Json.parseToJsonElement(capturedBody!!).jsonObject
        assertFalse(parsed.containsKey("locationBias"), "locationBias must NOT be present when near is null")
    }

    @Test
    fun `confirm resolves a prediction to coordinates and minimal cachedMetadata`() = runBlocking {
        // confirm() fetches place details for an unconfirmed prediction, filling in
        // coordinates and a minimal cachedMetadata snapshot — only the cacheable fields,
        // never the raw response (per the google retention rule, doc03.02.02).
        val placeJson = """
            {
              "id": "places/abc123",
              "displayName": {"text": "Test Place", "languageCode": "en"},
              "location": {"latitude": 1.0, "longitude": 2.0},
              "formattedAddress": "1 Test St",
              "types": ["restaurant"]
            }
        """.trimIndent()

        val provider = GoogleLocationProvider(
            client = mockClient(placeJson),
            apiKey = { "test-key" },
        )
        val stub = LocationCandidate(
            coordinates = com.saxonthune.ranktheplanet.domain.Coordinates(0.0, 0.0),
            displayName = "Test Place",
            sourceType = SourceType.Google,
            sourceId = "abc123",
            cachedMetadata = null,
            detail = null,
            needsConfirmation = true,
        )
        val result = provider.confirm(stub)
        assertIs<ProviderResult.Ok<LocationCandidate>>(result)
        val c = result.value
        assertEquals(1.0, c.coordinates.lat)
        assertEquals(2.0, c.coordinates.lng)
        assertFalse(c.needsConfirmation)

        val metadata = c.cachedMetadata
        assertNotNull(metadata)
        val parsed = Json.parseToJsonElement(metadata).jsonObject
        assertEquals("places/abc123", parsed["id"]?.jsonPrimitive?.content)
        assertEquals("Test Place", parsed["displayName"]?.jsonPrimitive?.content)
        assertEquals("1 Test St", parsed["formattedAddress"]?.jsonPrimitive?.content)
        assertFalse(parsed.containsKey("languageCode"), "raw DTO field languageCode must not appear in cachedMetadata")
        assertFalse(parsed.containsKey("location"), "raw location object must not appear in cachedMetadata")
    }
}
