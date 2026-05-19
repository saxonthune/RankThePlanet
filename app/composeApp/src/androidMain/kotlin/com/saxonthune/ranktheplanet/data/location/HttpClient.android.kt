package com.saxonthune.ranktheplanet.data.location

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual fun createOsmHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true })
    }
    defaultRequest {
        headers.append("User-Agent", OSM_USER_AGENT)
    }
}
