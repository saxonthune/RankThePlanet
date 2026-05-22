package com.saxonthune.ranktheplanet.data.location

import com.saxonthune.ranktheplanet.data.secure.SecureStore
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

class LocationProviderRegistryTest {

    private class FakeSecureStore : SecureStore {
        val storage = mutableMapOf<String, String>()
        var putCalls = 0

        override suspend fun get(key: String): String? = storage[key]
        override suspend fun put(key: String, value: String) {
            putCalls++
            storage[key] = value
        }
    }

    private fun throwingClient(): HttpClient {
        val engine = MockEngine { error("HTTP engine must not be invoked") }
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    @Test
    fun `Google provider returns NOT_CONFIGURED when store is empty`() = runBlocking {
        val store = FakeSecureStore()
        val registry = DefaultLocationProviderRegistry.create(store, throwingClient())
        val result = registry.providerFor(SourceType.Google)!!.resolve("test")
        assertIs<ProviderResult.Failed>(result)
        assertEquals(ProviderError.NOT_CONFIGURED, result.error)
    }

    @Test
    fun `saveProviderKey Google writes to store and header reaches provider`() = runBlocking {
        val store = FakeSecureStore()
        val capturedKeys = mutableListOf<String?>()
        val mockClient = HttpClient(MockEngine { request ->
            capturedKeys += request.headers["X-Goog-Api-Key"]
            respond(
                content = """{"places":[]}""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
            )
        }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val registry = DefaultLocationProviderRegistry.create(store, mockClient)
        registry.saveProviderKey(SourceType.Google, "abc")
        registry.providerFor(SourceType.Google)!!.resolve("test")

        assertEquals(1, store.putCalls)
        assertEquals("abc", capturedKeys.firstOrNull())
    }

    @Test
    fun `saveProviderKey for non-Google type does not call put`() = runBlocking {
        val store = FakeSecureStore()
        val registry = DefaultLocationProviderRegistry.create(store, throwingClient())
        registry.saveProviderKey(SourceType.Osm, "anything")
        assertEquals(0, store.putCalls)
    }
}
