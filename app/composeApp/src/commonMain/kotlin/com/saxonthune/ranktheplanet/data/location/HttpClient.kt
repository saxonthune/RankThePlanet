package com.saxonthune.ranktheplanet.data.location

import io.ktor.client.HttpClient

internal const val OSM_USER_AGENT = "RankThePlanet/1.0 (https://github.com/saxonthune/RankThePlanet)"

expect fun createOsmHttpClient(): HttpClient
