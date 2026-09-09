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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OsmLocationProviderTest {

    private fun mockClient(
        responseBody: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ): HttpClient {
        val engine = MockEngine {
            respond(
                content = responseBody,
                status = status,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString())
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }

    @Test
    fun `resolve maps photon feature collection to LocationCandidates`() = runBlocking {
        val photonJson = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {"type": "Point", "coordinates": [13.405, 52.52]},
                  "properties": {
                    "osm_id": 240109189,
                    "osm_type": "N",
                    "name": "Berlin",
                    "country": "Germany"
                  }
                }
              ]
            }
        """.trimIndent()

        val provider = OsmLocationProvider(mockClient(photonJson))
        val result = provider.resolve("Berlin")

        assertIs<ProviderResult.Ok<List<LocationCandidate>>>(result)
        val candidates = result.value
        assertEquals(1, candidates.size)
        val c = candidates[0]
        assertEquals(52.52, c.coordinates.lat)
        assertEquals(13.405, c.coordinates.lng)
        assertEquals(SourceType.Osm, c.sourceType)
        assertEquals("N240109189", c.sourceId)
        assertEquals("Berlin", c.displayName)
        assertEquals("Germany", c.detail)
    }

    @Test
    fun `resolveNearby maps photon reverse to candidates sorted by distance`() = runBlocking {
        // The far feature is listed first in the response; expect the near one first after sorting.
        val photonJson = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {"type": "Point", "coordinates": [13.410, 52.524]},
                  "properties": {"osm_id": 2, "osm_type": "N", "name": "Far Cafe"}
                },
                {
                  "type": "Feature",
                  "geometry": {"type": "Point", "coordinates": [13.4051, 52.5201]},
                  "properties": {"osm_id": 1, "osm_type": "N", "name": "Near Cafe"}
                }
              ]
            }
        """.trimIndent()

        val provider = OsmLocationProvider(mockClient(photonJson))
        val result = provider.resolveNearby(
            com.saxonthune.ranktheplanet.domain.Coordinates(lat = 52.52, lng = 13.405)
        )

        assertIs<ProviderResult.Ok<List<LocationCandidate>>>(result)
        val candidates = result.value
        assertEquals(2, candidates.size)
        assertEquals("Near Cafe", candidates[0].displayName)
        assertEquals("Far Cafe", candidates[1].displayName)
        assertEquals(SourceType.Osm, candidates[0].sourceType)
        assertEquals("N1", candidates[0].sourceId)
        // Distance is prepended to detail (e.g. "12 m" or "0.4 km").
        assertNotNull(candidates[0].detail)
        assert(candidates[0].detail!!.endsWith(" m") || candidates[0].detail!!.contains(" m ·")) {
            "Expected leading metres-distance in detail: ${candidates[0].detail}"
        }
    }

    @Test
    fun `resolveNearby hits the photon reverse endpoint with limit`() = runBlocking {
        val photonJson = """{"type":"FeatureCollection","features":[]}"""
        var capturedUrl: String? = null
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(
                content = photonJson,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        val provider = OsmLocationProvider(client)
        provider.resolveNearby(com.saxonthune.ranktheplanet.domain.Coordinates(lat = 52.52, lng = 13.405))

        assertNotNull(capturedUrl)
        assert(capturedUrl!!.contains("/reverse")) { "Expected /reverse path in URL: $capturedUrl" }
        assert(capturedUrl!!.contains("lat=52.52")) { "Expected lat=52.52 in URL: $capturedUrl" }
        assert(capturedUrl!!.contains("lon=13.405")) { "Expected lon=13.405 in URL: $capturedUrl" }
        assert(capturedUrl!!.contains("limit=10")) { "Expected limit=10 in URL: $capturedUrl" }
    }

    @Test
    fun `resolve returns Failed on non-2xx response`() = runBlocking {
        val provider = OsmLocationProvider(
            mockClient("{}", status = HttpStatusCode.InternalServerError)
        )
        val result = provider.resolve("anywhere")
        assertIs<ProviderResult.Failed>(result)
        assertEquals(ProviderError.PROVIDER_ERROR, result.error)
    }

    @Test
    fun `resolve with near appends lat and lon to the Photon URL`() = runBlocking {
        val photonJson = """{"type":"FeatureCollection","features":[]}"""
        var capturedUrl: String? = null
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(
                content = photonJson,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        val provider = OsmLocationProvider(client)
        provider.resolve(
            "coffee",
            bias = LocationBias.Point(com.saxonthune.ranktheplanet.domain.Coordinates(lat = 37.7749, lng = -122.4194)),
        )

        assertNotNull(capturedUrl)
        assert(capturedUrl!!.contains("lat=37.7749")) { "Expected lat=37.7749 in URL: $capturedUrl" }
        assert(capturedUrl!!.contains("lon=-122.4194")) { "Expected lon=-122.4194 in URL: $capturedUrl" }
    }

    @Test
    fun `resolve with box bias appends bbox to the Photon URL`() = runBlocking {
        val photonJson = """{"type":"FeatureCollection","features":[]}"""
        var capturedUrl: String? = null
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(
                content = photonJson,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        val provider = OsmLocationProvider(client)
        provider.resolve(
            "cinema",
            bias = LocationBias.Box(south = 40.70, west = -74.02, north = 40.80, east = -73.93),
        )

        assertNotNull(capturedUrl)
        // Photon expects west,south,east,north
        assert(capturedUrl!!.contains("bbox=-74.02%2C40.7%2C-73.93%2C40.8") || capturedUrl!!.contains("bbox=-74.02,40.7,-73.93,40.8")) {
            "Expected bbox in west,south,east,north order in URL: $capturedUrl"
        }
        assert(!capturedUrl!!.contains("lat=")) { "Expected no lat param when box bias is used: $capturedUrl" }
    }

    @Test
    fun `resolve without near does not append lat and lon to the Photon URL`() = runBlocking {
        val photonJson = """{"type":"FeatureCollection","features":[]}"""
        var capturedUrl: String? = null
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(
                content = photonJson,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        val provider = OsmLocationProvider(client)
        provider.resolve("coffee")

        assertNotNull(capturedUrl)
        assert(!capturedUrl!!.contains("lat=")) { "Expected no lat param in URL: $capturedUrl" }
        assert(!capturedUrl!!.contains("lon=")) { "Expected no lon param in URL: $capturedUrl" }
    }

    @Test
    fun `resolve returns sourceId null when photon feature lacks osm_id`() = runBlocking {
        val photonJson = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {"type": "Point", "coordinates": [2.3488, 48.8534]},
                  "properties": {
                    "name": "Interpolated address"
                  }
                }
              ]
            }
        """.trimIndent()

        val provider = OsmLocationProvider(mockClient(photonJson))
        val result = provider.resolve("somewhere")

        assertIs<ProviderResult.Ok<List<LocationCandidate>>>(result)
        assertNull(result.value[0].sourceId)
    }
}
