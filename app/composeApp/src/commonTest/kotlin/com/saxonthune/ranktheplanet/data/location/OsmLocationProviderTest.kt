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
        assertEquals("Berlin, Germany", c.displayName)
    }

    @Test
    fun `resolveNearby maps nominatim reverse response to LocationCandidate`() = runBlocking {
        val nominatimJson = """
            {
              "lat": "52.5200",
              "lon": "13.4050",
              "display_name": "Berlin, Deutschland",
              "osm_id": 62422,
              "osm_type": "relation"
            }
        """.trimIndent()

        val provider = OsmLocationProvider(mockClient(nominatimJson))
        val result = provider.resolveNearby(
            com.saxonthune.ranktheplanet.domain.Coordinates(lat = 52.52, lng = 13.405)
        )

        assertIs<ProviderResult.Ok<List<LocationCandidate>>>(result)
        val candidates = result.value
        assertEquals(1, candidates.size)
        val c = candidates[0]
        assertEquals(52.52, c.coordinates.lat)
        assertEquals(13.405, c.coordinates.lng)
        assertEquals(SourceType.Osm, c.sourceType)
        assertEquals("R62422", c.sourceId)
        assertEquals("Berlin, Deutschland", c.displayName)
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
